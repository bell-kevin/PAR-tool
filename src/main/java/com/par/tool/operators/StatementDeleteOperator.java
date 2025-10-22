package com.par.tool.operators;

import com.par.tool.MutationContext;
import com.par.tool.MutationOperator;
import com.par.tool.Patch;

import java.util.ArrayList;
import java.util.List;

public final class StatementDeleteOperator implements MutationOperator {
    @Override
    public List<Patch> generate(String originalSource, MutationContext context, int limit) {
        List<Patch> patches = new ArrayList<>();
        String[] lines = originalSource.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] copy = new String[lines.length - 1];
            System.arraycopy(lines, 0, copy, 0, i);
            System.arraycopy(lines, i + 1, copy, i, lines.length - i - 1);
            patches.add(new Patch(joinLines(copy), name() + " line=" + (i + 1)));
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
