package com.par.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FixDatabase {
    private final List<FixPattern> patterns;

    public FixDatabase() {
        List<FixPattern> list = new ArrayList<>();
        list.add(new NullCheckGuardFix());
        list.add(new NoneEqualityFix());
        list.add(new BoundsCheckFix());
        this.patterns = Collections.unmodifiableList(list);
    }

    public List<FixPattern> patterns() {
        return patterns;
    }

    private static String[] splitLines(String source) {
        return source.split("\\r?\\n", -1);
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

    private static String indentOf(String line) {
        int count = 0;
        while (count < line.length()) {
            char c = line.charAt(count);
            if (c == ' ' || c == '\t') {
                count++;
            } else {
                break;
            }
        }
        return line.substring(0, count);
    }

    private static String replaceLine(String[] lines, int index, String replacement) {
        String[] copy = lines.clone();
        copy[index] = replacement;
        return joinLines(copy);
    }

    private static String replaceLineWithBlock(String[] lines, int index, String block) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i == index) {
                sb.append(block);
            } else {
                sb.append(lines[i]);
            }
            if (i < lines.length - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static class NullCheckGuardFix extends FixPattern {
        private static final Pattern ACCESS = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\.([a-zA-Z_][a-zA-Z0-9_]*)\\(");

        NullCheckGuardFix() {
            super("NullCheckGuard", "Wrap attribute access in a None guard.");
        }

        @Override
        public List<Patch> apply(String source, MutationContext context, int limit) {
            List<Patch> patches = new ArrayList<>();
            String[] lines = splitLines(source);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.trim().isEmpty()) {
                    continue;
                }
                Matcher matcher = ACCESS.matcher(line);
                while (matcher.find()) {
                    String var = matcher.group(1);
                    String indent = indentOf(line);
                    String trimmed = line.trim();
                    String guarded = indent + "if " + var + " is not None:\n" + indent + "    " + trimmed;
                    String mutated = replaceLineWithBlock(lines, i, guarded);
                    patches.add(new Patch(mutated, "Null guard for " + var + "." + matcher.group(2)));
                    if (patches.size() >= limit) {
                        return patches;
                    }
                }
            }
            return patches;
        }
    }

    private static class NoneEqualityFix extends FixPattern {
        NoneEqualityFix() {
            super("NoneEquality", "Normalize None equality comparisons to identity checks.");
        }

        @Override
        public List<Patch> apply(String source, MutationContext context, int limit) {
            List<Patch> patches = new ArrayList<>();
            String[] lines = splitLines(source);
            Pattern eq = Pattern.compile("==\\s*None");
            Pattern ne = Pattern.compile("!=\\s*None");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                boolean changed = false;
                String updated = line;
                Matcher mEq = eq.matcher(updated);
                if (mEq.find()) {
                    updated = mEq.replaceAll("is None");
                    changed = true;
                }
                Matcher mNe = ne.matcher(updated);
                if (mNe.find()) {
                    updated = mNe.replaceAll("is not None");
                    changed = true;
                }
                if (changed) {
                    patches.add(new Patch(replaceLine(lines, i, updated), "Use identity check for None"));
                    if (patches.size() >= limit) {
                        return patches;
                    }
                }
            }
            return patches;
        }
    }

    private static class BoundsCheckFix extends FixPattern {
        private static final Pattern INDEX = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\[([a-zA-Z_][a-zA-Z0-9_]*)]");

        BoundsCheckFix() {
            super("BoundsGuard", "Add len-based bounds guard before index access.");
        }

        @Override
        public List<Patch> apply(String source, MutationContext context, int limit) {
            List<Patch> patches = new ArrayList<>();
            String[] lines = splitLines(source);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                Matcher matcher = INDEX.matcher(line);
                while (matcher.find()) {
                    String collection = matcher.group(1);
                    String index = matcher.group(2);
                    String indent = indentOf(line);
                    String trimmed = line.trim();
                    String guard = indent + "if 0 <= " + index + " < len(" + collection + "):\n" + indent + "    " + trimmed;
                    String mutated = replaceLineWithBlock(lines, i, guard);
                    patches.add(new Patch(mutated, "Bounds guard for " + collection + "[" + index + "]"));
                    if (patches.size() >= limit) {
                        return patches;
                    }
                }
            }
            return patches;
        }
    }
}
