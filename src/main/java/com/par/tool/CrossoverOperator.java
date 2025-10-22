package com.par.tool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CrossoverOperator {
    private CrossoverOperator() {}

    public static List<Patch> apply(List<Patch> seeds, int limit) {
        List<Patch> patches = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < seeds.size(); i++) {
            for (int j = i + 1; j < seeds.size(); j++) {
                if (patches.size() >= limit) {
                    return patches;
                }
                Patch a = seeds.get(i);
                Patch b = seeds.get(j);
                String combined = combine(a.source(), b.source());
                if (combined.equals(a.source()) || combined.equals(b.source())) {
                    continue;
                }
                if (seen.add(combined)) {
                    patches.add(new Patch(combined, "Crossover of [" + a.description() + "] + [" + b.description() + "]"));
                }
            }
        }
        return patches;
    }

    private static String combine(String a, String b) {
        String[] linesA = a.split("\n", -1);
        String[] linesB = b.split("\n", -1);
        int pivot = linesA.length / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pivot && i < linesA.length; i++) {
            sb.append(linesA[i]);
            sb.append('\n');
        }
        for (int i = pivot; i < linesB.length; i++) {
            sb.append(linesB[i]);
            if (i < linesB.length - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
