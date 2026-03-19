package com.melina.notes.controller;

import com.melina.notes.dto.NoteDTO;
import com.melina.notes.entity.Note;
import com.melina.notes.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteController {
    private final NoteService noteService;

    @PostMapping
    public ResponseEntity<NoteDTO> createNote(@RequestBody NoteDTO noteDTO) {
        NoteDTO note = noteService.createNote(noteDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    @GetMapping
    public ResponseEntity<List<NoteDTO>> getAllNotes(@RequestParam Long userId) {
        List<NoteDTO> notes = noteService.getAllNotes(userId);
        if (notes.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(notes);
    }

    @GetMapping("/{noteId}")
    public ResponseEntity<NoteDTO> getNoteById(@PathVariable Long noteId, @RequestParam Long userId) {
        NoteDTO note = noteService.getNoteById(userId,noteId);
        return ResponseEntity.ok(note);
    }

    @PatchMapping("/{noteId}")
    public ResponseEntity<NoteDTO> updateNote(@PathVariable Long noteId, @RequestBody NoteDTO noteDTO) {
        NoteDTO note = noteService.updateNote(noteId,noteDTO);
        return ResponseEntity.ok(note);
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long noteId) {
        noteService.deleteNote(noteId);
        return ResponseEntity.ok().build();
    }
}
