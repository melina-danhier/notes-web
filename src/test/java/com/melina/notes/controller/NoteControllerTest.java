package com.melina.notes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melina.notes.dto.CreateNoteDto;
import com.melina.notes.dto.NoteDto;
import com.melina.notes.exception.InvalidNoteStateException;
import com.melina.notes.exception.NoteNotFoundException;
import com.melina.notes.security.CustomUserDetails;
import com.melina.notes.service.NoteService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoteController.class)
class NoteControllerTest {

    @MockBean
    private NoteService noteService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createNote_withValidDto_returnsCreatedNoteAndCreatedStatus() throws Exception {
        CreateNoteDto note = createCreateNoteDto("Titel");
        NoteDto createdNote = createNoteDto(10L, "Titel");
        CustomUserDetails userDetails = createCustomUserDetails();

        when(noteService.createNoteForUser(eq(userDetails.getId()), any(CreateNoteDto.class)))
                .thenReturn(createdNote);

        mockMvc.perform(post("/api/notes")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(note)))
                .andExpectAll(
                        status().isCreated(),
                        jsonPath("$.id").value(createdNote.getId()),
                        jsonPath("$.title").value(createdNote.getTitle()),
                        jsonPath("$.content").value(createdNote.getContent())
                );

        verify(noteService, only()).createNoteForUser(eq(userDetails.getId()), any(CreateNoteDto.class));
    }

    @Test
    void createNote_withInvalidDto_returnsBadRequest() throws Exception {
        CreateNoteDto note = createCreateNoteDto(null);
        CustomUserDetails userDetails = createCustomUserDetails();

        mockMvc.perform(post("/api/notes")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(note)))
                .andExpect(status().isBadRequest());

        verify(noteService, never()).createNoteForUser(anyLong(), any(CreateNoteDto.class));
    }

    @Test
    void createNote_withMoreThanTenTags_returnsBadRequest() throws Exception {
        CreateNoteDto note = createCreateNoteDto("Titel");
        note.setTags(java.util.stream.IntStream.rangeClosed(1, 11)
                .mapToObj(number -> "tag" + number)
                .toList());
        CustomUserDetails userDetails = createCustomUserDetails();

        mockMvc.perform(post("/api/notes")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(note)))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.details[0]")
                                .value(org.hamcrest.Matchers.containsString("höchstens 10 Tags"))
                );

        verify(noteService, never()).createNoteForUser(anyLong(), any(CreateNoteDto.class));
    }

    @Test
    void getNoteById_withValidId_returnsOkWithNote() throws Exception {
        NoteDto note = createNoteDto(10L, "Titel");
        CustomUserDetails userDetails = createCustomUserDetails();

        when(noteService.getNoteById(anyLong(),anyLong())).thenReturn(note);

        mockMvc.perform(get("/api/notes/10")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(note.getId()),
                        jsonPath("$.title").value(note.getTitle()),
                        jsonPath("$.content").value(note.getContent())
                );

        verify(noteService).getNoteById(anyLong(),anyLong());
    }

    @Test
    void getNoteById_withInvalidId_returnsNotFound() throws Exception {
        CustomUserDetails userDetails = createCustomUserDetails();

        when(noteService.getNoteById(anyLong(),anyLong())).thenThrow(NoteNotFoundException.class);

        mockMvc.perform(get("/api/notes/10")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateNote_withValidDto_returnsOkAndUpdatedNote() throws Exception {
        CreateNoteDto updateRequestDto = createCreateNoteDto("Neuer Titel");
        NoteDto updatedNote = createNoteDto(10L, "Neuer Titel");
        CustomUserDetails userDetails = createCustomUserDetails();

        when(noteService.updateNoteForUser(anyLong(),anyLong(),any(CreateNoteDto.class)))
                .thenReturn(updatedNote);

        mockMvc.perform(patch("/api/notes/10")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestDto)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(10L),
                        jsonPath("$.title").value(updatedNote.getTitle())
                );

        verify(noteService).updateNoteForUser(anyLong(),anyLong(),any(CreateNoteDto.class));
    }

    @Test
    void updateNote_withInvalidDto_returnsBadRequest() throws Exception {
        CreateNoteDto updateRequestDto = createCreateNoteDto("Zu langer Titellllllllllllllllllllllllllllllllllllll");
        CustomUserDetails userDetails = createCustomUserDetails();

        mockMvc.perform(patch("/api/notes/10")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequestDto)))
                .andExpect(status().isBadRequest());

        verify(noteService, never()).updateNoteForUser(anyLong(),anyLong(),any(CreateNoteDto.class));
    }

    @Test
    void deleteNote_withValidId_returnsNoContent() throws Exception {
        CustomUserDetails userDetails = createCustomUserDetails();

        doNothing().when(noteService).deleteNoteForUser(anyLong(),anyLong());

        mockMvc.perform(delete("/api/notes/10")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().isNoContent());

        verify(noteService, only()).deleteNoteForUser(anyLong(),anyLong());
    }

    @Test
    void deleteNote_withInvalidId_returnsNotFound() throws Exception {
        CustomUserDetails userDetails = createCustomUserDetails();

        doThrow(NoteNotFoundException.class)
                .when(noteService).deleteNoteForUser(userDetails.getId(), 10L);

        mockMvc.perform(delete("/api/notes/10")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().isNotFound());

        verify(noteService).deleteNoteForUser(userDetails.getId(), 10L);
    }

    @Test
    void softDeleteNote_withValidId_returnsOk() throws Exception {
        CustomUserDetails userDetails = createCustomUserDetails();
        NoteDto note = createNoteDto(10L, "Titel");

        when(noteService.softDeleteNoteForUser(anyLong(),anyLong()))
                .thenReturn(note);

        mockMvc.perform(patch("/api/notes/10/delete")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(note.getId()),
                        jsonPath("$.title").value(note.getTitle()),
                        jsonPath("$.content").value(note.getContent())
                );

        verify(noteService).softDeleteNoteForUser(userDetails.getId(), 10L);
    }

    @Test
    void restoreNote_returnsOk() throws Exception {
        CustomUserDetails userDetails = createCustomUserDetails();
        NoteDto note = createNoteDto(10L, "Titel");
        when(noteService.restoreNoteForUser(userDetails.getId(), 10L)).thenReturn(note);

        mockMvc.perform(patch("/api/notes/10/restore").with(csrf()).with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(noteService).restoreNoteForUser(userDetails.getId(), 10L);
    }

    @Test
    void restoreNote_withInvalidState_returnsConflict() throws Exception {
        CustomUserDetails userDetails = createCustomUserDetails();
        when(noteService.restoreNoteForUser(userDetails.getId(), 10L))
                .thenThrow(new InvalidNoteStateException(10L, "Nicht im Papierkorb"));

        mockMvc.perform(patch("/api/notes/10/restore").with(csrf()).with(user(userDetails)))
                .andExpectAll(
                        status().isConflict(),
                        jsonPath("$.status").value("CONFLICT")
                );
    }

    @Test
    void createNote_withMalformedJson_returnsBadRequest() throws Exception {
        CustomUserDetails userDetails = createCustomUserDetails();
        String malformedJson = "{";

        mockMvc.perform(post("/api/notes")
                        .with(csrf())
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpectAll(
                        status().isBadRequest(),
                        jsonPath("$.message").value("Ungültiger Request-Body")
                );
    }

    @Test
    void unsupportedMethod_returnsMethodNotAllowed() throws Exception {
        CustomUserDetails userDetails = createCustomUserDetails();

        mockMvc.perform(post("/api/notes/10")
                        .with(csrf())
                        .with(user(userDetails)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void archiveNote_returnsOk() throws Exception {
        CustomUserDetails userDetails = createCustomUserDetails();
        NoteDto note = createNoteDto(10L, "Titel");
        when(noteService.archiveNoteForUser(userDetails.getId(), 10L)).thenReturn(note);

        mockMvc.perform(patch("/api/notes/10/archive").with(csrf()).with(user(userDetails)))
                .andExpect(status().isOk());

        verify(noteService).archiveNoteForUser(userDetails.getId(), 10L);
    }

    @Test
    void unarchiveNote_returnsOk() throws Exception {
        CustomUserDetails userDetails = createCustomUserDetails();
        NoteDto note = createNoteDto(10L, "Titel");
        when(noteService.unarchiveNoteForUser(userDetails.getId(), 10L)).thenReturn(note);

        mockMvc.perform(patch("/api/notes/10/unarchive").with(csrf()).with(user(userDetails)))
                .andExpect(status().isOk());

        verify(noteService).unarchiveNoteForUser(userDetails.getId(), 10L);
    }

    @Test
    void emptyTrash_returnsOk() throws Exception {
        CustomUserDetails userDetails = createCustomUserDetails();

        mockMvc.perform(delete("/api/notes/trash/empty").with(csrf()).with(user(userDetails)))
                .andExpect(status().isOk());

        verify(noteService).emptyTrashForUser(userDetails.getId());
    }

    @Test
    void togglePin_returnsOk() throws Exception {
        CustomUserDetails userDetails = createCustomUserDetails();
        NoteDto note = createNoteDto(10L, "Titel");
        when(noteService.togglePinNoteForUser(userDetails.getId(), 10L)).thenReturn(note);

        mockMvc.perform(patch("/api/notes/10/pin").with(csrf()).with(user(userDetails)))
                .andExpect(status().isOk());

        verify(noteService).togglePinNoteForUser(userDetails.getId(), 10L);
    }

    private CustomUserDetails createCustomUserDetails() {
        return new CustomUserDetails(
                10L,
                "user@mail.com",
                "password",
                "ROLE_USER"
        );
    }

    private CreateNoteDto createCreateNoteDto(String title) {
        return CreateNoteDto.builder()
                .title(title)
                .content("Inhalt")
                .tags(java.util.List.of("work"))
                .build();
    }

    private NoteDto createNoteDto(Long id, String title) {
        return NoteDto.builder()
                .id(id)
                .title(title)
                .content("Inhalt")
                .tags(java.util.List.of("work"))
                .build();
    }
}
