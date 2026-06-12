package com.melina.notes.controller;

import com.melina.notes.dto.CreateNoteDto;
import org.junit.jupiter.api.Test;

class NoteControllerTest {

    @Test
    void createNote() {
        CreateNoteDto note = createNoteDto();
    }

    private CreateNoteDto createNoteDto() {
        return CreateNoteDto.builder()
                .id(1L)
                .title("Titel")
                .content("Inhalt")
                .tags(java.util.List.of("work"))
                .build();
    }

    @Test
    void getNoteById() {
    }

    @Test
    void updateNote() {
    }

    @Test
    void deleteNote() {
    }

    @Test
    void softDeleteNote() {
    }

    @Test
    void restoreNote() {
    }

    @Test
    void archiveNote() {
    }

    @Test
    void unarchiveNote() {
    }

    @Test
    void emptyTrash() {
    }

    @Test
    void togglePin() {
    }
}
