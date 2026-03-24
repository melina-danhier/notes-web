package com.melina.notes.controller;

import com.melina.notes.dto.EditNoteDTO;
import com.melina.notes.dto.NoteDTO;
import com.melina.notes.service.NoteService;
import com.melina.notes.security.CustomUserDetails;
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
    public ResponseEntity<NoteDTO> createNote(@RequestBody EditNoteDTO noteDTO,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        NoteDTO note = noteService.createNoteForUser(userDetails.getId(),noteDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    @GetMapping("/{noteId}")
    public ResponseEntity<NoteDTO> getNoteById(@PathVariable Long noteId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        NoteDTO note = noteService.getNoteById(userDetails.getId(), noteId);
        return ResponseEntity.ok(note);
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<NoteDTO> updateNote(@PathVariable Long noteId,
                                              @RequestBody EditNoteDTO noteDTO,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        NoteDTO note = noteService.updateNoteForUser(userDetails.getId(), noteId, noteDTO);
        return ResponseEntity.ok(note);
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long noteId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        noteService.deleteNoteForUser(userDetails.getId(), noteId);
        return ResponseEntity.ok().build();
    }
}