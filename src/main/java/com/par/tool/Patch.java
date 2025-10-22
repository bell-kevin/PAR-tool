package com.par.tool;

public final class Patch {
    private final String source;
    private final String description;

    public Patch(String source, String description) {
        this.source = source;
        this.description = description;
    }

    public String source() {
        return source;
    }

    public String description() {
        return description;
    }
}
