package com.mycompany.tutorhub_enterprise.client.ai.patch;

import java.util.Arrays;
import java.util.List;

public final class DiffGenerator {

    private DiffGenerator() {
    }

    public static String unifiedDiff(String relativePath, String originalContent, String proposedContent) {
        List<String> oldLines = splitLines(originalContent);
        List<String> newLines = splitLines(proposedContent);

        int prefix = commonPrefix(oldLines, newLines);
        int suffix = commonSuffix(oldLines, newLines, prefix);
        int oldStart = Math.max(0, prefix);
        int oldEnd = oldLines.size() - suffix;
        int newEnd = newLines.size() - suffix;

        StringBuilder diff = new StringBuilder();
        String path = relativePath == null || relativePath.isBlank() ? "file" : relativePath;
        diff.append("--- a/").append(path).append('\n');
        diff.append("+++ b/").append(path).append('\n');
        diff.append("@@ -").append(oldStart + 1).append(',').append(Math.max(0, oldEnd - oldStart))
                .append(" +").append(oldStart + 1).append(',').append(Math.max(0, newEnd - oldStart))
                .append(" @@\n");

        int contextStart = Math.max(0, oldStart - 3);
        for (int i = contextStart; i < oldStart; i++) {
            diff.append(' ').append(oldLines.get(i)).append('\n');
        }
        for (int i = oldStart; i < oldEnd; i++) {
            diff.append('-').append(oldLines.get(i)).append('\n');
        }
        for (int i = oldStart; i < newEnd; i++) {
            diff.append('+').append(newLines.get(i)).append('\n');
        }
        int contextEnd = Math.min(oldLines.size(), oldEnd + 3);
        for (int i = oldEnd; i < contextEnd; i++) {
            diff.append(' ').append(oldLines.get(i)).append('\n');
        }
        return diff.toString();
    }

    private static List<String> splitLines(String value) {
        String normalized = value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
        return Arrays.asList(normalized.split("\n", -1));
    }

    private static int commonPrefix(List<String> left, List<String> right) {
        int max = Math.min(left.size(), right.size());
        int index = 0;
        while (index < max && left.get(index).equals(right.get(index))) {
            index++;
        }
        return index;
    }

    private static int commonSuffix(List<String> left, List<String> right, int prefix) {
        int leftIndex = left.size() - 1;
        int rightIndex = right.size() - 1;
        int count = 0;
        while (leftIndex >= prefix && rightIndex >= prefix && left.get(leftIndex).equals(right.get(rightIndex))) {
            count++;
            leftIndex--;
            rightIndex--;
        }
        return count;
    }
}
