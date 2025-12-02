package com.par.tool;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessUtilsTest {
    @Test
    void runCommandCapturesStdoutAndExitCode() throws Exception {
        TestRunResult result = ProcessUtils.runCommand(printCommand("hello"), Path.of("."), 5);
        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("hello"));
        assertEquals("", result.stderr().trim());
    }

    @Test
    void runCommandCapturesStderrOnFailure() throws Exception {
        TestRunResult result = ProcessUtils.runCommand(stderrCommand("oops"), Path.of("."), 5);
        assertEquals(3, result.exitCode());
        assertTrue(result.stderr().contains("oops"));
    }

    @Test
    void runCommandReturnsTimeoutWhenExceeded() throws Exception {
        TestRunResult result = ProcessUtils.runCommand(sleepCommand(2), Path.of("."), 1);
        assertEquals(124, result.exitCode());
        assertTrue(result.stderr().contains("TIMEOUT"));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static String printCommand(String message) {
        return isWindows() ? "echo " + message : "echo '" + message + "'";
    }

    private static String stderrCommand(String message) {
        if (isWindows()) {
            return "echo " + message + " 1>&2 & exit /b 3";
        }
        return "sh -c 'echo " + message + " 1>&2; exit 3'";
    }

    private static String sleepCommand(int seconds) {
        if (isWindows()) {
            return "powershell -Command \"Start-Sleep -Seconds " + seconds + "\"";
        }
        return "sleep " + seconds;
    }
}
