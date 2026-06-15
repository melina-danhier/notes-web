package com.melina.notes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentPreviewUtilTest {

    @Test
    void createPreview_nullOrBlankContent_returnsEmptyString() {
        assertEquals("", ContentPreviewUtil.createPreview(null));
        assertEquals("", ContentPreviewUtil.createPreview(" \n\t "));
    }

    @Test
    void createPreview_removesBoundaryWhitespaceAndKeepsInternalBlankLines() {
        String content = "\n  Erster Absatz  \n\n  Zweiter Absatz  \n";

        String preview = ContentPreviewUtil.createPreview(content);

        assertEquals("Erster Absatz\n\nZweiter Absatz", preview);
    }

    @Test
    void createPreview_limitsPhysicalLines() {
        String content = "eins\n\nzwei\ndrei\nvier\nfünf";

        String preview = ContentPreviewUtil.createPreview(content, 4);

        assertEquals("eins\n\nzwei\ndrei", preview);
    }

    @Test
    void createPreview_limitsCharactersAndAddsEllipsis() {
        String preview = ContentPreviewUtil.createPreview("a".repeat(600));

        assertEquals(500, preview.length());
        assertTrue(preview.endsWith("..."));
    }

    @Test
    void createPreview_invalidLimits_throwException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ContentPreviewUtil.createPreview("Inhalt", 0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ContentPreviewUtil.createPreview("Inhalt", 1, 2)
        );
    }
}
