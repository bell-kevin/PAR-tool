package com.par.tool.operators;

import com.par.tool.MutationContext;
import com.par.tool.MutationOperator;
import com.par.tool.Patch;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SmallIntTweakerOperator implements MutationOperator {
    private static final Pattern SMALL_INT = Pattern.compile("(?<![\\w-])(-?[0-3])(?![\\w])");

    @Override
    public List<Patch> generate(String originalSource, MutationContext context, int limit) {
        List<Patch> patches = new ArrayList<>();
        String[] lines = originalSource.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = SMALL_INT.matcher(line);
            while (matcher.find()) {
                int value = Integer.parseInt(matcher.group(1));
                int tweaked = value >= 0 ? value + 1 : value - 1;
                String updated = matcher.replaceFirst(String.valueOf(tweaked));
                String[] copy = lines.clone();
                copy[i] = updated;
                patches.add(new Patch(joinLines(copy), name() + " line=" + (i + 1) + " value=" + value + "->" + tweaked));
                if (patches.size() >= limit) {
                    return patches;
                }
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
