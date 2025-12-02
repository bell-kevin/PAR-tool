package com.par.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void parseRejectsNonPositiveBudget() throws Exception {
        Path target = createTargetFile();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                Config.parse(new String[]{"--target", target.toString(), "--tests", "echo ok", "--budget", "0"}));

        assertTrue(ex.getMessage().contains("--budget"));
    }

    @Test
    void parseRejectsNonPositiveTimeout() throws Exception {
        Path target = createTargetFile();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                Config.parse(new String[]{"--target", target.toString(), "--tests", "echo ok", "--timeout", "-5"}));

        assertTrue(ex.getMessage().contains("--timeout"));
    }

    private Path createTargetFile() throws IOException {
        Path target = tempDir.resolve("module.py");
        Files.writeString(target, "print('ok')\n");
        return target;
    }
}
