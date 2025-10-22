package com.par.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class ProcessUtils {
    private ProcessUtils() {}

    public static TestRunResult runCommand(String command, Path cwd, int timeoutSeconds) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("bash", "-lc", command);
        builder.directory(cwd.toFile());
        Process process = builder.start();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> stdout = executor.submit(streamToString(process.inputReader(StandardCharsets.UTF_8)));
            Future<String> stderr = executor.submit(streamToString(process.errorReader(StandardCharsets.UTF_8)));

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new TestRunResult(124, getSafely(stdout), getSafely(stderr) + "\nTIMEOUT");
            }
            int exit = process.exitValue();
            return new TestRunResult(exit, getSafely(stdout), getSafely(stderr));
        } finally {
            executor.shutdownNow();
        }
    }

    private static Callable<String> streamToString(BufferedReader reader) {
        return () -> {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        };
    }

    private static String getSafely(Future<String> future) {
        try {
            return future.get();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException ex) {
            return "";
        }
    }
}
