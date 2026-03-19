package com.melina.notes.service;

import com.melina.notes.dto.NoteDTO;
import com.melina.notes.entity.Note;
import com.melina.notes.exception.NoteNotFoundException;
import com.melina.notes.exception.UserNoteMismatchException;
import com.melina.notes.mapper.NoteMapper;
import com.melina.notes.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {
    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;

    public NoteDTO createNote(NoteDTO noteDTO) {
        Note note = new Note();
        noteMapper.updateNote(note,noteDTO);
        note = noteRepository.save(note);
        return noteMapper.toNoteDTO(note);
    }

    public List<NoteDTO> getAllNotes(Long userId) {
        List<Note> notes = noteRepository.findAllByUser_Id(userId);
        return noteMapper.toNoteDTO(notes);
    }

    public NoteDTO getNoteById(Long userId, Long noteId) {
        Note note = getNote(noteId);
        if (note.getUser().getId().equals(userId)) {
            throw new UserNoteMismatchException("User with id " + userId + " is not owner of Note with id " + noteId);
        }
        return noteMapper.toNoteDTO(note);
    }

    public NoteDTO updateNote(Long noteId, NoteDTO noteDTO) {
        Note note = getNote(noteId);
        noteMapper.updateNote(note,noteDTO);
        return noteMapper.toNoteDTO(note);
    }

    public void deleteNote(Long noteId) {
        Note note = getNote(noteId);
        noteRepository.delete(note);
    }

    private Note getNote(Long noteId) {
        return noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException("Note with id not found: " + noteId));
    }
}
