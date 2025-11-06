package com.par.tool;

public final class FaultPattern {
    private final String name;
    private final String description;
    private final String detectorKey;

    public FaultPattern(String name, String description, String detectorKey) {
        this.name = name;
        this.description = description;
        this.detectorKey = detectorKey;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String detectorKey() {
        return detectorKey;
    }
}
