package com.melina.notes.util;

import java.util.ArrayList;
import java.util.List;

public final class ContentPreviewUtil {

    private static final int DEFAULT_MAX_LINES = 5;
    private static final int DEFAULT_MAX_CHARACTERS = 500;
    private static final String ELLIPSIS = "...";

    private ContentPreviewUtil() {
    }

    public static String createPreview(String content) {
        return createPreview(content, DEFAULT_MAX_LINES, DEFAULT_MAX_CHARACTERS);
    }

    public static String createPreview(String content, int maxLines) {
        return createPreview(content, maxLines, DEFAULT_MAX_CHARACTERS);
    }

    static String createPreview(String content, int maxLines, int maxCharacters) {
        if (maxLines < 1) {
            throw new IllegalArgumentException("maxLines muss mindestens 1 sein");
        }
        if (maxCharacters < ELLIPSIS.length()) {
            throw new IllegalArgumentException("maxCharacters muss mindestens 3 sein");
        }
        if (content == null || content.isBlank()) {
            return "";
        }

        String[] lines = content.replace("\r\n", "\n")
                .replace('\r', '\n')
                .split("\n", -1);

        int firstContentLine = findFirstContentLine(lines);
        int lastContentLine = findLastContentLine(lines);
        List<String> previewLines = collectPreviewLines(lines, firstContentLine, lastContentLine, maxLines);
        String preview = String.join("\n", previewLines);

        if (preview.length() <= maxCharacters) {
            return preview;
        }
        return preview.substring(0, maxCharacters - ELLIPSIS.length()).stripTrailing() + ELLIPSIS;
    }

    private static int findFirstContentLine(String[] lines) {
        int index = 0;
        while (index < lines.length && lines[index].isBlank()) {
            index++;
        }
        return index;
    }

    private static int findLastContentLine(String[] lines) {
        int index = lines.length - 1;
        while (index >= 0 && lines[index].isBlank()) {
            index--;
        }
        return index;
    }

    private static List<String> collectPreviewLines(
            String[] lines,
            int firstContentLine,
            int lastContentLine,
            int maxLines
    ) {
        List<String> previewLines = new ArrayList<>();
        int endExclusive = Math.min(firstContentLine + maxLines, lastContentLine + 1);

        for (int index = firstContentLine; index < endExclusive; index++) {
            previewLines.add(lines[index].strip());
        }

        while (!previewLines.isEmpty() && previewLines.getLast().isBlank()) {
            previewLines.removeLast();
        }
        return previewLines;
    }
}
