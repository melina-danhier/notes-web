package com.melina.notes.util;

public class ContentPreviewUtil {

    private static final int DEFAULT_MAX_LINES = 5;

    /**
     * Erstellt eine saubere Vorschau aus dem Notiz-Inhalt:
     * - Extrahiert die ersten maxLines Zeilen
     * - Entfernt führende und nachfolgende Leerzeichen
     * - Entfernt vollständig leere Zeilen am Anfang/Ende
     * - Behält Zeilenumbrüche bei
     */
    public static String createPreview(String content) {
        return createPreview(content, DEFAULT_MAX_LINES);
    }

    public static String createPreview(String content, int maxLines) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        // Zerteile in Zeilen
        String[] lines = content.split("\n");
        StringBuilder preview = new StringBuilder();
        int lineCount = 0;

        for (String line : lines) {
            String trimmedLine = line.trim();

            // Ignoriere vollständig leere Zeilen
            if (trimmedLine.isEmpty()) {
                continue;
            }

            // Wenn wir genug Zeilen haben, stoppe
            if (lineCount >= maxLines) {
                break;
            }

            // Füge Zeilenumbruch hinzu (außer für die erste Zeile)
            if (lineCount > 0) {
                preview.append("\n");
            }

            // Füge die Zeile hinzu
            preview.append(trimmedLine);
            lineCount++;
        }

        return preview.toString();
    }
}

