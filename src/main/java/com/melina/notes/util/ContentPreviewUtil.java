package com.melina.notes.util;

public class ContentPreviewUtil {

    private static final int DEFAULT_MAX_LENGTH = 250;

    /**
     * Erstellt eine saubere Vorschau aus dem Notiz-Inhalt:
     * - Entfernt führende und nachfolgende Leerzeichen
     * - Entfernt vollständig leere Zeilen
     * - Begrenzt die Länge auf maxLength Zeichen
     * - Behält echte Zeilenumbrüche bei
     */
    public static String createPreview(String content) {
        return createPreview(content, DEFAULT_MAX_LENGTH);
    }

    public static String createPreview(String content, int maxLength) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        // Zerteile in Zeilen, filtere leere Zeilen und beschneide
        String[] lines = content.split("\n");
        StringBuilder preview = new StringBuilder();
        int charCount = 0;

        for (String line : lines) {
            String trimmedLine = line.trim();

            // Ignoriere vollständig leere Zeilen (aber behalte Zeilenumbruch bei)
            if (trimmedLine.isEmpty()) {
                continue;
            }

            // Wenn wir bereits genug Zeichen haben, stoppe
            if (charCount > 0 && charCount + 1 + trimmedLine.length() > maxLength) {
                break;
            }

            // Füge Zeilenumbruch hinzu (außer für die erste Zeile)
            if (charCount > 0) {
                preview.append("\n");
                charCount += 1;
            }

            // Füge die Zeile hinzu (oder einen abgeschnittenen Teil davon)
            if (charCount + trimmedLine.length() > maxLength) {
                int remainingChars = maxLength - charCount;
                preview.append(trimmedLine.substring(0, remainingChars)).append("…");
                charCount = maxLength;
            } else {
                preview.append(trimmedLine);
                charCount += trimmedLine.length();
            }

            // Wenn wir das Limit erreichthaben, stoppe
            if (charCount >= maxLength) {
                break;
            }
        }

        return preview.toString();
    }
}

