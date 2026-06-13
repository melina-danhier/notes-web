package com.melina.notes.controller;

import com.melina.notes.dto.CreateNoteDto;
import com.melina.notes.dto.NoteDto;
import com.melina.notes.service.NoteService;
import com.melina.notes.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {
    private final NoteService noteService;

    @PostMapping
    public ResponseEntity<NoteDto> createNote(@Valid @RequestBody CreateNoteDto noteDTO,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        NoteDto note = noteService.createNoteForUser(userDetails.getId(), noteDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    @GetMapping("/{noteId}")
    public ResponseEntity<NoteDto> getNoteById(@PathVariable Long noteId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        NoteDto note = noteService.getNoteById(userDetails.getId(), noteId);
        return ResponseEntity.ok(note);
    }

    @PatchMapping("/{noteId}")
    public ResponseEntity<NoteDto> updateNote(@PathVariable Long noteId,
                                              @Valid @RequestBody CreateNoteDto noteDTO,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        NoteDto note = noteService.updateNoteForUser(userDetails.getId(), noteId, noteDTO);
        return ResponseEntity.ok(note);
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long noteId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        noteService.deleteNoteForUser(userDetails.getId(), noteId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{noteId}/delete")
    public ResponseEntity<NoteDto> softDeleteNote(@PathVariable Long noteId,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        NoteDto note = noteService.softDeleteNoteForUser(userDetails.getId(), noteId);
        return ResponseEntity.ok(note);
    }

    @PatchMapping("/{noteId}/restore")
    public ResponseEntity<NoteDto> restoreNote(@PathVariable Long noteId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        NoteDto note = noteService.restoreNoteForUser(userDetails.getId(), noteId);
        return ResponseEntity.ok(note);
    }

    @PatchMapping("/{noteId}/archive")
    public ResponseEntity<NoteDto> archiveNote(@PathVariable Long noteId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        NoteDto note = noteService.archiveNoteForUser(userDetails.getId(), noteId);
        return ResponseEntity.ok(note);
    }

    @PatchMapping("/{noteId}/unarchive")
    public ResponseEntity<NoteDto> unarchiveNote(@PathVariable Long noteId,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        NoteDto note = noteService.unarchiveNoteForUser(userDetails.getId(), noteId);
        return ResponseEntity.ok(note);
    }

    @DeleteMapping("/trash/empty")
    public ResponseEntity<Void> emptyTrash(@AuthenticationPrincipal CustomUserDetails userDetails) {
        noteService.emptyTrashForUser(userDetails.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{noteId}/pin")
    public ResponseEntity<NoteDto> togglePin(@PathVariable Long noteId,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        NoteDto note = noteService.togglePinNoteForUser(userDetails.getId(), noteId);
        return ResponseEntity.ok(note);
    }
}
