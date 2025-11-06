package com.par.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreTest {
    @Test
    void evaluatesJsonSummaryWhenAvailable() {
        String stdout = "{\"summary\": {\"failed\": 2, \"errors\": 1, \"passed\": 5}}";
        TestRunResult result = new TestRunResult(1, stdout, "");
        Score.ScoreResult score = Score.evaluate(result);
        assertEquals(2, score.failed());
        assertEquals(1, score.errors());
        assertEquals(5, score.passed());
        assertEquals(3, score.score());
        assertEquals("failed=2, errors=1, passed=5", score.summary());
    }

    @Test
    void returnsExitZeroWhenTestsPass() {
        String stdout = "3 passed, 1 skipped in 0.10s";
        TestRunResult result = new TestRunResult(0, stdout, "");
        Score.ScoreResult score = Score.evaluate(result);
        assertEquals(0, score.score());
        assertEquals(0, score.failed());
        assertEquals(0, score.errors());
        assertEquals(3, score.passed());
        assertEquals("failed=0, errors=0, passed=3, skipped=1", score.summary());
    }

    @Test
    void defaultsWhenSummaryMissing() {
        TestRunResult result = new TestRunResult(1, "unexpected output", "");
        Score.ScoreResult score = Score.evaluate(result);
        assertEquals(10000, score.score());
        assertEquals("no summary", score.summary());
    }
}
