package com.par.tool;

import java.util.regex.Pattern;

public final class FaultPattern {
    private final String name;
    private final String description;
    private final Pattern pattern;

    public FaultPattern(String name, String description, Pattern pattern) {
        this.name = name;
        this.description = description;
        this.pattern = pattern;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Pattern pattern() {
        return pattern;
    }
}
