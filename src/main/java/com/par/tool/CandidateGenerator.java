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
        if (limit <= 0 || operators.isEmpty()) {
            return patches;
        }
        int remaining = limit;
        int operatorCount = operators.size();
        for (int i = 0; i < operatorCount && remaining > 0; i++) {
            MutationOperator operator = operators.get(i);
            int operatorsLeft = operatorCount - i;
            int perOperatorLimit = Math.max(1, remaining / operatorsLeft);
            perOperatorLimit = Math.min(perOperatorLimit, remaining);
            List<Patch> generated = operator.generate(source, context, perOperatorLimit);
            for (Patch patch : generated) {
                if (patches.size() >= limit) {
                    break;
                }
                patches.add(patch);
            }
            remaining = limit - patches.size();
        }
        if (patches.size() < limit) {
            List<Patch> cross = CrossoverOperator.apply(patches, limit - patches.size());
            patches.addAll(cross);
        }
        return patches;
    }
}
