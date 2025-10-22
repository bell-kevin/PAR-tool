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
            if (patches.size() >= limit) {
                break;
            }
            MutationOperator operator = operators.get(index);
            int remainingOperators = operatorCount - index;
            int remainingBudget = limit - patches.size();
            int perOperatorLimit = Math.max(
                    (int) Math.ceil((double) remainingBudget / remainingOperators),
                    1);
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
