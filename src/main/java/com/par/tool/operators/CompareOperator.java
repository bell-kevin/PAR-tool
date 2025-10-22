package com.par.tool.operators;

import com.par.tool.MutationContext;
import com.par.tool.MutationOperator;
import com.par.tool.Patch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CompareOperator implements MutationOperator {
    private static final Map<String, String> REPLACEMENTS = new LinkedHashMap<>();

    static {
        REPLACEMENTS.put(">=", ">");
        REPLACEMENTS.put("<=", "<");
        REPLACEMENTS.put(">", ">=");
        REPLACEMENTS.put("<", "<=");
        REPLACEMENTS.put("==", "!=");
        REPLACEMENTS.put("!=", "==");
    }

    @Override
    public List<Patch> generate(String originalSource, MutationContext context, int limit) {
        List<Patch> patches = new ArrayList<>();
        String[] lines = originalSource.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            for (Map.Entry<String, String> entry : REPLACEMENTS.entrySet()) {
                String token = entry.getKey();
                String replacement = entry.getValue();
                int index = line.indexOf(token);
                if (index >= 0) {
                    String updated = line.substring(0, index) + replacement + line.substring(index + token.length());
                    String[] copy = lines.clone();
                    copy[i] = updated;
                    patches.add(new Patch(joinLines(copy), name() + " line=" + (i + 1) + " replace=" + token + "->" + replacement));
                    if (patches.size() >= limit) {
                        return patches;
                    }
                }
            }
        }
        return patches;
    }

    private static String joinLines(String[] lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(lines[i]);
            if (i < lines.length - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
