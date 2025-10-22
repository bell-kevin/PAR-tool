package com.par.tool;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Score {
    private static final Pattern NUMBER_TOKEN = Pattern.compile("(\\d+)\\s+(failed|errors?|passed|skipped|xfailed|xpassed)");

    private Score() {}

    public static ScoreResult evaluate(TestRunResult result) {
        if (result.exitCode() == 0) {
            return new ScoreResult(0, 0, 0, -1, "exit 0");
        }
        Map<String, Integer> stats = parseSummary(result.stdout() + "\n" + result.stderr());
        int failed = stats.getOrDefault("failed", 0);
        int errors = stats.getOrDefault("errors", 0);
        int passed = stats.getOrDefault("passed", -1);
        int score = failed + errors;
        if (score <= 0) {
            score = 9999;
        }
        return new ScoreResult(score, failed, errors, passed, summaryLine(stats));
    }

    private static Map<String, Integer> parseSummary(String text) {
        Map<String, Integer> stats = new HashMap<>();
        String[] lines = text.split("\n");
        String candidate = "";
        for (String line : lines) {
            if (line.contains("failed") || line.contains("error") || line.contains("passed")) {
                candidate = line;
            }
        }
        Matcher matcher = NUMBER_TOKEN.matcher(candidate);
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String key = matcher.group(2).toLowerCase();
            if (key.startsWith("fail")) {
                stats.put("failed", value);
            } else if (key.startsWith("error")) {
                stats.put("errors", value);
            } else if (key.startsWith("pass")) {
                stats.put("passed", value);
            } else if (key.startsWith("skip")) {
                stats.put("skipped", value);
            } else if (key.startsWith("xfail")) {
                stats.put("xfailed", value);
            } else if (key.startsWith("xpass")) {
                stats.put("xpassed", value);
            }
        }
        return stats;
    }

    private static String summaryLine(Map<String, Integer> stats) {
        return "failed=" + stats.getOrDefault("failed", 0) + ", errors=" + stats.getOrDefault("errors", 0);
    }

    public static final class ScoreResult {
        private final int score;
        private final int failed;
        private final int errors;
        private final int passed;
        private final String summary;

        public ScoreResult(int score, int failed, int errors, int passed, String summary) {
            this.score = score;
            this.failed = failed;
            this.errors = errors;
            this.passed = passed;
            this.summary = summary;
        }

        public int score() {
            return score;
        }

        public int failed() {
            return failed;
        }

        public int errors() {
            return errors;
        }

        public int passed() {
            return passed;
        }

        public String summary() {
            return summary;
        }
    }
}
