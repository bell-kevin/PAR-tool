package com.par.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class FaultDatabase {
    private final List<FaultPattern> patterns;

    public FaultDatabase() {
        List<FaultPattern> list = new ArrayList<>();
        list.add(new FaultPattern(
                "NullDereference",
                "Potential missing null/None guard before attribute access.",
                Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\.([a-zA-Z_][a-zA-Z0-9_]*)\\(")
        ));
        list.add(new FaultPattern(
                "LooseNoneEquality",
                "Equality comparison to None using == instead of 'is'.",
                Pattern.compile("==\\s*None|!=\\s*None")
        ));
        list.add(new FaultPattern(
                "UnsafeIndex",
                "Index access without explicit bounds check.",
                Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\[([a-zA-Z_][a-zA-Z0-9_]*)]")
        ));
        this.patterns = Collections.unmodifiableList(list);
    }

    public List<FaultPattern> patterns() {
        return patterns;
    }
}
