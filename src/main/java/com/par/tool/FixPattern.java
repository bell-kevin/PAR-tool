package com.par.tool;

import java.util.List;

public abstract class FixPattern {
    private final String name;
    private final String description;

    protected FixPattern(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public abstract List<Patch> apply(String source, MutationContext context, int limit);
}
