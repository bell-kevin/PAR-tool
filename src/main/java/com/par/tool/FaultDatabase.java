package com.par.tool;

import java.util.List;

public final class FaultDatabase {
    private final List<FaultPattern> patterns;

    public FaultDatabase() {
        this.patterns = List.of(
                new FaultPattern(
                        "NullDereference",
                        "Potential missing null/None guard before attribute access.",
                        "null_dereference"
                ),
                new FaultPattern(
                        "LooseNoneEquality",
                        "Equality comparison to None using == instead of 'is'.",
                        "loose_none_equality"
                ),
                new FaultPattern(
                        "UnsafeIndex",
                        "Index access without explicit bounds check.",
                        "unsafe_index"
                )
        );
    }

    public List<FaultPattern> patterns() {
        return patterns;
    }
}
