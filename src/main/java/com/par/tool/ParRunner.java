package com.par.tool;

import com.par.tool.operators.ArithmeticOperator;
import com.par.tool.operators.CompareOperator;
import com.par.tool.operators.IfNegationOperator;
import com.par.tool.operators.PatternBasedOperator;
import com.par.tool.operators.SmallIntTweakerOperator;
import com.par.tool.operators.StatementDeleteOperator;
import com.par.tool.operators.StatementDuplicateOperator;
import com.par.tool.operators.StatementSwapOperator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.par.tool.MutationOperator;

public final class ParRunner {
    private final Config config;
    private final FaultDatabase faultDatabase;
    private final FixDatabase fixDatabase;
    private final PatternMatcher patternMatcher;
    private final CandidateGenerator candidateGenerator;

    public ParRunner(Config config) {
        this.config = config;
        this.faultDatabase = new FaultDatabase();
        this.fixDatabase = new FixDatabase();
        this.patternMatcher = new PatternMatcher(faultDatabase, fixDatabase);
        List<MutationOperator> operators = new ArrayList<>();
        operators.add(new StatementDuplicateOperator());
        operators.add(new StatementDeleteOperator());
        operators.add(new StatementSwapOperator());
        operators.add(new ArithmeticOperator());
        operators.add(new CompareOperator());
        operators.add(new IfNegationOperator());
        operators.add(new SmallIntTweakerOperator());
        operators.add(new PatternBasedOperator(patternMatcher));
        this.candidateGenerator = new CandidateGenerator(operators);
    }

    public void run() throws IOException, InterruptedException {
        Path resultsDir = Path.of("_apr_results");
        Files.createDirectories(resultsDir);

        Path tempRoot = Files.createTempDirectory("apr_java_");
        Path projectName = config.getProject().getFileName();
        if (projectName == null) {
            throw new IllegalStateException("Project path has no file name");
        }
        Path workingCopy = tempRoot.resolve(projectName);
        FileUtils.copyRecursive(config.getProject(), workingCopy);
        try {
        Path relativeTarget = config.getProject().relativize(config.getTarget());
        Path targetCopy = workingCopy.resolve(relativeTarget);
        if (!Files.exists(targetCopy)) {
            throw new IllegalStateException("Target file not found in working copy: " + targetCopy);
        }

            TestRunResult baselineRun = ProcessUtils.runCommand(config.getTestsCommand(), workingCopy, config.getTimeoutSeconds());
            Score.ScoreResult baselineScore = Score.evaluate(baselineRun);
            System.out.println("BASELINE EXIT: " + baselineRun.exitCode());
            System.out.println("BASELINE SUMMARY: " + baselineScore.summary());
            if (baselineRun.exitCode() == 127) {
                System.out.println("Test command failed to launch (exit 127). Ensure the shell command is available and the tests command is valid.");
                writeBaselineLogs(resultsDir, baselineRun);
                String summary = SummaryWriter.createSummary(
                        "test_command_failed",
                        baselineRun.exitCode(),
                        baselineScore,
                        baselineScore,
                        0,
                        null,
                        Collections.emptyList()
                );
                Files.writeString(resultsDir.resolve("summary.json"), summary);
                return;
            }
            if (baselineRun.exitCode() == 0) {
                String summary = SummaryWriter.createSummary(
                        "already_passing",
                        baselineRun.exitCode(),
                        baselineScore,
                        baselineScore,
                        0,
                        null,
                        patternMatcher.detectFaults(Files.readString(targetCopy))
                );
                Files.writeString(resultsDir.resolve("summary.json"), summary);
                System.out.println("All tests already pass. Nothing to repair.");
                return;
            }

            String originalSource = Files.readString(targetCopy);
            Random random = new Random(config.getSeed());
            MutationContext context = new MutationContext(random, faultDatabase, fixDatabase);
            List<String> detectedFaults = patternMatcher.detectFaults(originalSource);
            if (!detectedFaults.isEmpty()) {
                System.out.println("Detected fault patterns: " + detectedFaults);
            }
            int candidateLimit = Math.max(config.getBudget() * 3, config.getBudget() + 10);
            List<Patch> candidates = candidateGenerator.generateCandidates(originalSource, context, candidateLimit);
            Collections.shuffle(candidates, random);

            System.out.printf("Detected %d logical processors; using %d worker threads.%n", config.getDetectedProcessors(), config.getThreads());
            List<Path> workerCopies = prepareWorkerCopies(workingCopy, tempRoot.resolve("workers"), projectName, config.getThreads());

            AtomicInteger attempts = new AtomicInteger();
            AtomicBoolean foundFix = new AtomicBoolean(false);
            AtomicReference<Score.ScoreResult> bestScore = new AtomicReference<>(baselineScore);
            AtomicReference<String> bestSource = new AtomicReference<>(null);
            AtomicReference<String> bestDescription = new AtomicReference<>(null);
            AtomicReference<String> bestDiff = new AtomicReference<>(null);
            Object bestLock = new Object();

            ExecutorService executor = Executors.newFixedThreadPool(config.getThreads());
            try {
                for (Patch patch : candidates) {
                    if (attempts.get() >= config.getBudget() || foundFix.get()) {
                        break;
                    }
                    Patch candidate = patch;
                    executor.submit(() -> {
                        if (foundFix.get()) {
                            return;
                        }
                        int attemptNumber = attempts.incrementAndGet();
                        if (attemptNumber > config.getBudget()) {
                            return;
                        }

                        Path workerCopy = workerCopies.get((attemptNumber - 1) % workerCopies.size());
                        Path workerTarget = workerCopy.resolve(relativeTarget);
                        try {
                            Files.writeString(workerTarget, candidate.source());
                            TestRunResult attempt = ProcessUtils.runCommand(config.getTestsCommand(), workerCopy, config.getTimeoutSeconds());
                            Score.ScoreResult attemptScore = Score.evaluate(attempt);
                            System.out.printf("[%d/%d] %s -> exit=%d score=%d summary=%s%n",
                                    attemptNumber,
                                    config.getBudget(),
                                    candidate.description(),
                                    attempt.exitCode(),
                                    attemptScore.score(),
                                    attemptScore.summary());

                            synchronized (bestLock) {
                                Score.ScoreResult currentBest = bestScore.get();
                                if (attemptScore.score() < currentBest.score()) {
                                    Path patchedName = Path.of(config.getTarget().toString() + " (patched)");
                                    String diff = FileUtils.computeDiff(originalSource, candidate.source(), config.getTarget(), patchedName);
                                    bestScore.set(attemptScore);
                                    bestSource.set(candidate.source());
                                    bestDescription.set(candidate.description());
                                    bestDiff.set(diff);
                                }
                            }

                            if (attempt.exitCode() == 0) {
                                System.out.println("🎉 Found a full fix!");
                                foundFix.set(true);
                            }
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        } finally {
                            try {
                                Files.writeString(workerTarget, originalSource);
                            } catch (IOException ignore) {
                                // If we fail to reset the target, subsequent attempts may still succeed because each worker has an isolated copy.
                            }
                        }
                    });
                }
            } finally {
                executor.shutdown();
                long waitSeconds = Math.max((long) config.getTimeoutSeconds() * config.getBudget(), config.getTimeoutSeconds());
                if (!executor.awaitTermination(waitSeconds + 30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            }

            String status;
            Score.ScoreResult best = bestScore.get();
            if (best.score() == 0) {
                status = "fixed";
            } else if (best.score() < baselineScore.score()) {
                status = "improved";
            } else {
                status = "no_fix";
            }

            String summary = SummaryWriter.createSummary(
                    status,
                    baselineRun.exitCode(),
                    baselineScore,
                    best,
                    attempts.get(),
                    bestDescription.get(),
                    detectedFaults
            );
            Files.writeString(resultsDir.resolve("summary.json"), summary);
            String bestSourceText = bestSource.get();
            if (bestSourceText != null) {
                Files.writeString(resultsDir.resolve("best_patch.py"), bestSourceText);
            }
            String diffText = bestDiff.get();
            if (diffText != null && !diffText.isBlank()) {
                Files.writeString(resultsDir.resolve("best_patch.diff"), diffText);
            }
        } finally {
            FileUtils.deleteRecursive(tempRoot);
        }
    }

    private void writeBaselineLogs(Path resultsDir, TestRunResult baselineRun) throws IOException {
        Files.writeString(resultsDir.resolve("baseline_stdout.log"), baselineRun.stdout());
        Files.writeString(resultsDir.resolve("baseline_stderr.log"), baselineRun.stderr());
    }

    private List<Path> prepareWorkerCopies(Path workingCopy, Path workersRoot, Path projectName, int threads) throws IOException {
        List<Path> copies = new ArrayList<>();
        Files.createDirectories(workersRoot);
        for (int i = 0; i < threads; i++) {
            Path workerRoot = workersRoot.resolve("worker_" + i);
            Path workerProject = workerRoot.resolve(projectName);
            FileUtils.copyRecursive(workingCopy, workerProject);
            copies.add(workerProject);
        }
        return copies;
    }
}
