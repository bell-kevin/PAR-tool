package com.par.tool.operators;

import com.par.tool.MutationContext;
import com.par.tool.MutationOperator;
import com.par.tool.Patch;

import java.util.ArrayList;
import java.util.List;

public final class IfNegationOperator implements MutationOperator {
    @Override
    public List<Patch> generate(String originalSource, MutationContext context, int limit) {
        List<Patch> patches = new ArrayList<>();
        String[] lines = originalSource.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.startsWith("if ") && trimmed.endsWith(":")) {
                String indent = line.substring(0, line.indexOf(trimmed));
                String condition = trimmed.substring(3, trimmed.length() - 1).trim();
                String rewritten;
                if (condition.startsWith("not ")) {
                    rewritten = indent + "if " + condition.substring(4).trim() + ":";
                } else {
                    rewritten = indent + "if not (" + condition + "):";
                }
                String[] copy = lines.clone();
                copy[i] = rewritten;
                patches.add(new Patch(joinLines(copy), name() + " line=" + (i + 1)));
                if (patches.size() >= limit) {
                    break;
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
