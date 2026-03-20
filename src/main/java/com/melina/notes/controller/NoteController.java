package com.melina.notes.controller;

import com.melina.notes.dto.NoteDTO;
import com.melina.notes.service.NoteService;
import com.melina.notes.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {
    private final NoteService noteService;

    @PostMapping
    public ResponseEntity<NoteDTO> createNote(@RequestBody NoteDTO noteDTO,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        NoteDTO note = noteService.createNoteForUser(noteDTO, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    @GetMapping
    public ResponseEntity<List<NoteDTO>> getAllNotes(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<NoteDTO> notes = noteService.getAllNotes(userDetails.getId());
        if (notes.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(notes);
    }

    @GetMapping("/{noteId}")
    public ResponseEntity<NoteDTO> getNoteById(@PathVariable Long noteId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        NoteDTO note = noteService.getNoteById(userDetails.getId(), noteId);
        return ResponseEntity.ok(note);
    }

    @PatchMapping("/{noteId}")
    public ResponseEntity<NoteDTO> updateNote(@PathVariable Long noteId,
                                              @RequestBody NoteDTO noteDTO,
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