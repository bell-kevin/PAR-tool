package com.par.tool.operators;

import com.par.tool.MutationContext;
import com.par.tool.MutationOperator;
import com.par.tool.Patch;

import java.util.ArrayList;
import java.util.List;

public final class StatementSwapOperator implements MutationOperator {
    @Override
    public List<Patch> generate(String originalSource, MutationContext context, int limit) {
        List<Patch> patches = new ArrayList<>();
        String[] lines = originalSource.split("\n", -1);
        for (int i = 0; i < lines.length - 1; i++) {
            String line = lines[i];
            String next = lines[i + 1];
            if (line.trim().isEmpty() || next.trim().isEmpty()) {
                continue;
            }
            String[] copy = lines.clone();
            copy[i] = next;
            copy[i + 1] = line;
            patches.add(new Patch(joinLines(copy), name() + " lines=" + (i + 1) + "/" + (i + 2)));
            if (patches.size() >= limit) {
                break;
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
