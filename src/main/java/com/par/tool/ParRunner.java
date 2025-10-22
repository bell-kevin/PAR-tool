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

            Score.ScoreResult bestScore = baselineScore;
            String bestSource = null;
            String bestDescription = null;
            String bestDiff = null;
            int tried = 0;

            for (Patch patch : candidates) {
                if (tried >= config.getBudget()) {
                    break;
                }
                tried++;
                Files.writeString(targetCopy, patch.source());
                TestRunResult attempt = ProcessUtils.runCommand(config.getTestsCommand(), workingCopy, config.getTimeoutSeconds());
                Score.ScoreResult attemptScore = Score.evaluate(attempt);
                System.out.printf("[%d/%d] %s -> exit=%d score=%d summary=%s%n",
                        tried,
                        config.getBudget(),
                        patch.description(),
                        attempt.exitCode(),
                        attemptScore.score(),
                        attemptScore.summary());
                if (attemptScore.score() < bestScore.score()) {
                    bestScore = attemptScore;
                    bestSource = patch.source();
                    bestDescription = patch.description();
                    Path patchedName = Path.of(config.getTarget().toString() + " (patched)");
                    bestDiff = FileUtils.computeDiff(originalSource, patch.source(), config.getTarget(), patchedName);
                }
                if (attempt.exitCode() == 0) {
                    System.out.println("🎉 Found a full fix!");
                    break;
                }
            }

            Files.writeString(targetCopy, originalSource);

            String status;
            if (bestScore.score() == 0) {
                status = "fixed";
            } else if (bestScore.score() < baselineScore.score()) {
                status = "improved";
            } else {
                status = "no_fix";
            }

            String summary = SummaryWriter.createSummary(
                    status,
                    baselineRun.exitCode(),
                    baselineScore,
                    bestScore,
                    tried,
                    bestDescription,
                    detectedFaults
            );
            Files.writeString(resultsDir.resolve("summary.json"), summary);
            if (bestSource != null) {
                Files.writeString(resultsDir.resolve("best_patch.py"), bestSource);
            }
            if (bestDiff != null && !bestDiff.isBlank()) {
                Files.writeString(resultsDir.resolve("best_patch.diff"), bestDiff);
            }
        } finally {
            FileUtils.deleteRecursive(tempRoot);
        }
    }
}
