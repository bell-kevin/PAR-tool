package com.par.tool.operators;

import com.par.tool.MutationContext;
import com.par.tool.MutationOperator;
import com.par.tool.Patch;

import java.util.ArrayList;
import java.util.List;

public final class StatementDuplicateOperator implements MutationOperator {
    @Override
    public List<Patch> generate(String originalSource, MutationContext context, int limit) {
        List<Patch> patches = new ArrayList<>();
        String[] lines = originalSource.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] copy = lines.clone();
            String indent = indentOf(line);
            copy[i] = line;
            String[] extended = new String[copy.length + 1];
            System.arraycopy(copy, 0, extended, 0, i + 1);
            extended[i + 1] = indent + copyLine(line);
            System.arraycopy(copy, i + 1, extended, i + 2, copy.length - (i + 1));
            patches.add(new Patch(joinLines(extended), name() + " line=" + (i + 1)));
            if (patches.size() >= limit) {
                break;
            }
        }
        return patches;
    }

    private static String indentOf(String line) {
        int count = 0;
        while (count < line.length() && Character.isWhitespace(line.charAt(count))) {
            count++;
        }
        return line.substring(0, count);
    }

    private static String copyLine(String line) {
        return line.trim();
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
