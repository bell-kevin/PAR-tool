package com.par.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

public final class FileUtils {
    private FileUtils() {}

    public static void copyRecursive(Path source, Path target) throws IOException {
        if (!Files.exists(target)) {
            Files.createDirectories(target);
        }
        try (var stream = Files.walk(source)) {
            stream.forEach(path -> {
                try {
                    Path relative = source.relativize(path);
                    Path dest = target.resolve(relative);
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }
    }

    public static void deleteRecursive(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
        }
    }

    public static String computeDiff(String original, String mutated, Path originalFile, Path patchedFile) {
        String[] origLines = original.split("\n", -1);
        String[] newLines = mutated.split("\n", -1);
        StringBuilder diff = new StringBuilder();
        diff.append("--- ").append(originalFile).append('\n');
        diff.append("+++ ").append(patchedFile).append('\n');
        int max = Math.max(origLines.length, newLines.length);
        for (int i = 0; i < max; i++) {
            String o = i < origLines.length ? origLines[i] : "";
            String n = i < newLines.length ? newLines[i] : "";
            if (!o.equals(n)) {
                diff.append("@@ line ").append(i + 1).append(" @@").append('\n');
                if (!o.isEmpty()) {
                    diff.append('-').append(o).append('\n');
                }
                if (!n.isEmpty()) {
                    diff.append('+').append(n).append('\n');
                }
            }
        }
        return diff.toString();
    }
}
