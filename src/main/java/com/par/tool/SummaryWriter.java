package com.par.tool;

import java.util.List;

public final class SummaryWriter {
    private SummaryWriter() {}

    public static String createSummary(
            String status,
            int baselineExit,
            Score.ScoreResult baselineScore,
            Score.ScoreResult bestScore,
            int tried,
            String bestDescription,
            List<String> detectedFaults) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"status\": \"").append(escape(status)).append("\",\n");
        sb.append("  \"baseline\": {\n");
        sb.append("    \"exit\": ").append(baselineExit).append(",\n");
        sb.append("    \"failed\": ").append(baselineScore.failed()).append(",\n");
        sb.append("    \"errors\": ").append(baselineScore.errors()).append(",\n");
        sb.append("    \"passed\": ").append(baselineScore.passed()).append('\n');
        sb.append("  },\n");
        sb.append("  \"best\": {\n");
        sb.append("    \"score\": ").append(bestScore.score()).append(",\n");
        sb.append("    \"failed\": ").append(bestScore.failed()).append(",\n");
        sb.append("    \"errors\": ").append(bestScore.errors()).append(",\n");
        sb.append("    \"description\": \"").append(escape(bestDescription == null ? "" : bestDescription)).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"tried\": ").append(tried).append(",\n");
        sb.append("  \"detected_faults\": [");
        for (int i = 0; i < detectedFaults.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('\"').append(escape(detectedFaults.get(i))).append('\"');
        }
        sb.append("]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
