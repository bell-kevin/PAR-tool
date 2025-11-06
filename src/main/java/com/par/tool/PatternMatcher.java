package com.par.tool;

import java.util.ArrayList;
import java.util.List;

public final class PatternMatcher {
    private final FaultDatabase faultDatabase;
    private final FixDatabase fixDatabase;

    public PatternMatcher(FaultDatabase faultDatabase, FixDatabase fixDatabase) {
        this.faultDatabase = faultDatabase;
        this.fixDatabase = fixDatabase;
    }

    public List<String> detectFaults(String source) {
        List<String> matches = new ArrayList<>();
        for (FaultPattern pattern : faultDatabase.patterns()) {
            int occurrences = PythonAstService.countFaultOccurrences(source, pattern.detectorKey());
            if (occurrences > 0) {
                matches.add(pattern.name());
            }
        }
        return matches;
    }

    public List<Patch> createFixes(String source, MutationContext context, int limit) {
        List<Patch> patches = new ArrayList<>();
        for (FixPattern pattern : fixDatabase.patterns()) {
            if (patches.size() >= limit) {
                break;
            }
            List<Patch> produced = pattern.apply(source, context, limit - patches.size());
            patches.addAll(produced);
        }
        return patches;
    }
}
