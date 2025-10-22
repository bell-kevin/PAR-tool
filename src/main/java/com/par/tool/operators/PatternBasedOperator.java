package com.par.tool.operators;

import com.par.tool.MutationContext;
import com.par.tool.MutationOperator;
import com.par.tool.Patch;
import com.par.tool.PatternMatcher;

import java.util.List;

public final class PatternBasedOperator implements MutationOperator {
    private final PatternMatcher matcher;

    public PatternBasedOperator(PatternMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public List<Patch> generate(String originalSource, MutationContext context, int limit) {
        return matcher.createFixes(originalSource, context, limit);
    }

    @Override
    public String name() {
        return "PatternBasedOperator";
    }
}
