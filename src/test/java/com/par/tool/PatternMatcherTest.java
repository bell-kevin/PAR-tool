package com.par.tool;

import com.par.tool.operators.PatternBasedOperator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternMatcherTest {
    private final FaultDatabase faultDatabase = new FaultDatabase();
    private final FixDatabase fixDatabase = new FixDatabase();
    private final PatternMatcher matcher = new PatternMatcher(faultDatabase, fixDatabase);

    @Test
    void detectsFaultsUsingAstAnalysis() {
        String source = String.join("\n",
                "def demo(items, idx, foo, value):",
                "    items[idx] = 10",
                "    foo.bar()",
                "    if value == None:",
                "        return True",
                "    return False"
        );
        List<String> faults = matcher.detectFaults(source);
        assertTrue(faults.contains("NullDereference"));
        assertTrue(faults.contains("LooseNoneEquality"));
        assertTrue(faults.contains("UnsafeIndex"));
    }

    @Test
    void generatesAstBackedFixes() {
        String source = String.join("\n",
                "def demo(items, idx, foo, value):",
                "    items[idx] = 10",
                "    foo.bar()",
                "    if value == None:",
                "        return True",
                "    return False"
        );
        MutationContext context = new MutationContext(new Random(0), faultDatabase, fixDatabase);
        PatternBasedOperator operator = new PatternBasedOperator(matcher);
        List<Patch> patches = operator.generate(source, context, 6);
        assertTrue(patches.stream().anyMatch(p -> p.source().contains("if foo is not None")),
                "expected null-guard patch");
        assertTrue(patches.stream().anyMatch(p -> p.source().contains("is None")),
                "expected None equality normalization");
        assertTrue(patches.stream().anyMatch(p -> p.source().contains("0 <= idx < len(items)")),
                "expected bounds guard patch");
    }
}
