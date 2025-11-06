package com.par.tool;

import java.util.List;

public final class FixDatabase {
    private final List<FixPattern> patterns;

    public FixDatabase() {
        this.patterns = List.of(
                new AstFixPattern("NullCheckGuard", "Wrap attribute access in a None guard.", "null_guard"),
                new AstFixPattern("NoneEquality", "Normalize None equality comparisons to identity checks.", "none_identity"),
                new AstFixPattern("BoundsGuard", "Add len-based bounds guard before index access.", "bounds_guard")
        );
    }

    public List<FixPattern> patterns() {
        return patterns;
    }

    private static final class AstFixPattern extends FixPattern {
        private final String fixKey;

        AstFixPattern(String name, String description, String fixKey) {
            super(name, description);
            this.fixKey = fixKey;
        }

        @Override
        public List<Patch> apply(String source, MutationContext context, int limit) {
            return PythonAstService.applyFix(source, fixKey, limit);
        }
    }
}
