package com.melina.notes.service;

import com.melina.notes.dto.CreateUpdateNoteDto;
import com.melina.notes.dto.NoteDto;
import com.melina.notes.entity.Note;
import com.melina.notes.exception.NoteNotFoundException;
import com.melina.notes.mapper.NoteMapper;
import com.melina.notes.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class NoteService {
    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;

    public NoteDto create(CreateUpdateNoteDto noteDto) {
        Note note = noteMapper.toEntity(noteDto);
        note = noteRepository.save(note);
        return noteMapper.toDto(note);
    }

    public List<NoteDto> getAll() {
        List<Note> allNotes = noteRepository.findAll();
        return allNotes.stream()
                .map(noteMapper::toDto)
                .toList();
    }

    public NoteDto get(Long id) {
        Note note = getNoteEntity(id);
        return noteMapper.toDto(note);
    }

    public NoteDto update(Long id, CreateUpdateNoteDto noteDto) {
        Note note = getNoteEntity(id);
        noteMapper.update(note,noteDto);
        note = noteRepository.save(note);
        return noteMapper.toDto(note);
    }

    public void delete(Long id) {
        noteRepository.deleteById(id);
    }

    private Note getNoteEntity(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));
    }
}
