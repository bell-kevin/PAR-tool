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
        int operatorCount = operators.size();
        for (int index = 0; index < operatorCount; index++) {
            MutationOperator operator = operators.get(index);
            if (patches.size() >= limit) {
                break;
            }
            int remaining = limit - patches.size();
            int operatorsLeft = operatorCount - index;
            // Distribute the remaining budget so that no single operator can exhaust it.
            int perOperatorLimit = Math.max(remaining / operatorsLeft, 1);
            perOperatorLimit = Math.min(perOperatorLimit, remaining);
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
