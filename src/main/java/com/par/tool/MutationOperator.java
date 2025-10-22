package com.par.tool;

import java.util.List;

public interface MutationOperator {
    List<Patch> generate(String originalSource, MutationContext context, int limit);

    default String name() {
        return getClass().getSimpleName();
    }
}
