package com.par.tool;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessUtilsTest {
    @Test
    void runCommandCapturesStdoutAndExitCode() throws Exception {
        TestRunResult result = ProcessUtils.runCommand("python3 -c \"print('hello')\"", Path.of("."), 5);
        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("hello"));
        assertEquals("", result.stderr().trim());
    }

    @Test
    void runCommandCapturesStderrOnFailure() throws Exception {
        TestRunResult result = ProcessUtils.runCommand(
                "python3 -c \"import sys; print('oops', file=sys.stderr); sys.exit(3)\"",
                Path.of("."),
                5
        );
        assertEquals(3, result.exitCode());
        assertTrue(result.stderr().contains("oops"));
    }

    @Test
    void runCommandReturnsTimeoutWhenExceeded() throws Exception {
        TestRunResult result = ProcessUtils.runCommand(
                "python3 -c \"import time; time.sleep(2)\"",
                Path.of("."),
                1
        );
        assertEquals(124, result.exitCode());
        assertTrue(result.stderr().contains("TIMEOUT"));
    }
}
