package com.par.tool;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Score {
    private static final Pattern NUMBER_TOKEN = Pattern.compile("(\\d+)\\s+(failed|errors?|passed|skipped|xfailed|xpassed)");
    private static final Pattern JSON_TOKEN = Pattern.compile("\"(failed|errors?|passed|skipped|xfailed|xpassed)\"\\s*:\\s*(\\d+)");

    private Score() {}

    public static ScoreResult evaluate(TestRunResult result) {
        String combined = result.stdout() + "\n" + result.stderr();
        Map<String, Integer> stats = parseJsonSummary(combined);
        if (stats.isEmpty()) {
            stats = parseSummary(combined);
        }
        if (result.exitCode() == 0) {
            return new ScoreResult(0, 0, 0, stats.getOrDefault("passed", -1),
                    stats.isEmpty() ? "exit 0" : summaryLine(stats));
        }
        int failed = stats.getOrDefault("failed", 0);
        int errors = stats.getOrDefault("errors", 0);
        int passed = stats.getOrDefault("passed", -1);
        int score = failed + errors;
        if (score <= 0) {
            score = 10000;
        }
        String summary = stats.isEmpty() ? "no summary" : summaryLine(stats);
        return new ScoreResult(score, failed, errors, passed, summary);
    }

    private static Map<String, Integer> parseJsonSummary(String text) {
        Map<String, Integer> stats = new HashMap<>();
        Matcher matcher = JSON_TOKEN.matcher(text);
        while (matcher.find()) {
            String key = normalizeKey(matcher.group(1));
            int value = Integer.parseInt(matcher.group(2));
            stats.put(key, value);
        }
        return stats;
    }

    private static Map<String, Integer> parseSummary(String text) {
        Map<String, Integer> stats = new HashMap<>();
        Matcher matcher = NUMBER_TOKEN.matcher(text);
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String key = normalizeKey(matcher.group(2));
            stats.put(key, value);
        }
        return stats;
    }

    private static String summaryLine(Map<String, Integer> stats) {
        StringBuilder sb = new StringBuilder();
        appendSummary(sb, "failed", stats.getOrDefault("failed", 0));
        appendSummary(sb, "errors", stats.getOrDefault("errors", 0));
        if (stats.containsKey("passed")) {
            appendSummary(sb, "passed", stats.get("passed"));
        }
        if (stats.containsKey("skipped")) {
            appendSummary(sb, "skipped", stats.get("skipped"));
        }
        if (stats.containsKey("xfailed")) {
            appendSummary(sb, "xfailed", stats.get("xfailed"));
        }
        if (stats.containsKey("xpassed")) {
            appendSummary(sb, "xpassed", stats.get("xpassed"));
        }
        return sb.toString();
    }

    private static void appendSummary(StringBuilder sb, String key, int value) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(key).append('=').append(value);
    }

    private static String normalizeKey(String token) {
        String lower = token.toLowerCase();
        if (lower.startsWith("fail")) {
            return "failed";
        }
        if (lower.startsWith("error")) {
            return "errors";
        }
        if (lower.startsWith("pass")) {
            return "passed";
        }
        if (lower.startsWith("skip")) {
            return "skipped";
        }
        if (lower.startsWith("xfail")) {
            return "xfailed";
        }
        if (lower.startsWith("xpass")) {
            return "xpassed";
        }
        return lower;
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
