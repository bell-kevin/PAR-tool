package com.par.tool;

import java.util.Random;

public final class MutationContext {
    private final Random random;
    private final FaultDatabase faultDatabase;
    private final FixDatabase fixDatabase;

    public MutationContext(Random random, FaultDatabase faultDatabase, FixDatabase fixDatabase) {
        this.random = random;
        this.faultDatabase = faultDatabase;
        this.fixDatabase = fixDatabase;
    }

    public Random random() {
        return random;
    }

    public FaultDatabase faultDatabase() {
        return faultDatabase;
    }

    public FixDatabase fixDatabase() {
        return fixDatabase;
    }
}
