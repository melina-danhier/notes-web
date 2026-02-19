package com.melina.notes.controller;

import com.melina.notes.dto.CreateUpdateNoteDto;
import com.melina.notes.dto.NoteDto;
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
    public ResponseEntity<NoteDto> createNote(@RequestBody CreateUpdateNoteDto note) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.create(note));
    }

    @GetMapping
    public ResponseEntity<List<NoteDto>> getNotes() {
        return ResponseEntity.ok()
                .body(noteService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteDto> getNote(@PathVariable Long id) {
        return ResponseEntity.ok()
                .body(noteService.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteDto> updateNote(@PathVariable Long id,
                                              @RequestBody CreateUpdateNoteDto noteDto) {
        return ResponseEntity.ok()
                .body(noteService.update(id, noteDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        noteService.delete(id);
        return ResponseEntity.ok().build();
    }

}
