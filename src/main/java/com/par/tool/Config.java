package com.par.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class Config {
    private final Path project;
    private final Path target;
    private final String testsCommand;
    private final int budget;
    private final int timeoutSeconds;
    private final long seed;

    private Config(Path project, Path target, String testsCommand, int budget, int timeoutSeconds, long seed) {
        this.project = project;
        this.target = target;
        this.testsCommand = testsCommand;
        this.budget = budget;
        this.timeoutSeconds = timeoutSeconds;
        this.seed = seed;
    }

    public static Config parse(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                String key = arg.substring(2);
                String value = "true";
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    value = args[++i];
                }
                options.put(key, value);
            }
        }

        String targetValue = options.get("target");
        if (targetValue == null) {
            throw new IllegalArgumentException("Missing required --target <file> argument");
        }
        Path target = Path.of(targetValue).toAbsolutePath().normalize();
        if (!Files.exists(target)) {
            throw new IllegalArgumentException("Target file does not exist: " + target);
        }

        Path project;
        if (options.containsKey("project")) {
            project = Path.of(options.get("project")).toAbsolutePath().normalize();
        } else {
            project = target.getParent();
        }
        if (project == null) {
            throw new IllegalArgumentException("Could not determine project root");
        }
        if (!Files.exists(project) || !Files.isDirectory(project)) {
            throw new IllegalArgumentException("Project directory does not exist: " + project);
        }

        String tests = options.get("tests");
        if (tests == null || tests.isBlank()) {
            throw new IllegalArgumentException("Missing required --tests <command> argument");
        }

        int budget = parseInt(options.getOrDefault("budget", "200"), 200, "budget");
        if (budget <= 0) {
            throw new IllegalArgumentException("--budget must be a positive integer, received: " + budget);
        }

        int timeout = parseInt(options.getOrDefault("timeout", "120"), 120, "timeout");
        if (timeout <= 0) {
            throw new IllegalArgumentException("--timeout must be a positive integer, received: " + timeout);
        }
        long seed = parseLong(options.getOrDefault("seed", "1337"), 1337L, "seed");

        return new Config(project, target, tests, budget, timeout, seed);
    }

    private static int parseInt(String value, int defaultValue, String option) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid integer for --" + option + ": " + value);
        }
    }

    private static long parseLong(String value, long defaultValue, String option) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid long for --" + option + ": " + value);
        }
    }

    public Path getProject() {
        return project;
    }

    public Path getTarget() {
        return target;
    }

    public String getTestsCommand() {
        return testsCommand;
    }

    public int getBudget() {
        return budget;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public long getSeed() {
        return seed;
    }
}
