package com.par.tool;

import java.util.ArrayList;
import java.util.List;

public final class CandidateGenerator {
    private final List<MutationOperator> operators;

    public CandidateGenerator(List<MutationOperator> operators) {
        this.operators = operators;
    }

    public List<Patch> generateCandidates(String source, MutationContext context, int limit) {
        List<Patch> patches = new ArrayList<>();
        int perOperatorLimit = Math.max(limit, 20);
        for (MutationOperator operator : operators) {
            if (patches.size() >= limit) {
                break;
            }
            List<Patch> generated = operator.generate(source, context, perOperatorLimit);
            for (Patch patch : generated) {
                if (patches.size() >= limit) {
                    break;
                }
                patches.add(patch);
            }
        }
        if (patches.size() < limit) {
            List<Patch> cross = CrossoverOperator.apply(patches, limit - patches.size());
            patches.addAll(cross);
        }
        return patches;
    }
}
