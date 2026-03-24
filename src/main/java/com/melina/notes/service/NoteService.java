package com.melina.notes.service;

import com.melina.notes.dto.EditNoteDTO;
import com.melina.notes.dto.NoteDTO;
import com.melina.notes.entity.Note;
import com.melina.notes.exception.NoteNotFoundException;
import com.melina.notes.exception.UserNoteMismatchException;
import com.melina.notes.mapper.NoteMapper;
import com.melina.notes.repository.NoteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteService {
    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;
    private final TagService tagService;
    private final UserService userService;

    public NoteDTO createNoteForUser(Long userId, EditNoteDTO noteDTO) {
        Note note = Note.builder()
                .title(noteDTO.getTitle())
                .content(noteDTO.getContent())
                .created(Instant.now())
                .updated(Instant.now())
                .tags(tagService.getOrCreateTags(noteDTO.getTagsRaw()))
                .user(userService.getUser(userId))
                .build();
        note = noteRepository.save(note);
        return noteMapper.toNoteDTO(note);
    }

    public List<NoteDTO> getAllNotesByUserId(Long userId) {
        List<Note> notes = noteRepository.findAllByUser_Id(userId);
        return noteMapper.toNoteDTO(notes);
    }

    public NoteDTO getNoteById(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);
        return noteMapper.toNoteDTO(note);
    }

    public EditNoteDTO getNoteByIdForView(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);
        return noteMapper.toEditNoteDto(note);
    }

    public NoteDTO updateNoteForUser(Long userId, Long noteId, EditNoteDTO noteDTO) {
        Note note = getNote(userId, noteId);
        noteMapper.updateNote(note,noteDTO);
        note.setTags(tagService.getOrCreateTags(noteDTO.getTagsRaw()));
        note.setUpdated(Instant.now());
        note = noteRepository.save(note);
        return noteMapper.toNoteDTO(note);
    }

    public void deleteNoteForUser(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);
        noteRepository.delete(note);
    }

    private Note getNote(Long userId, Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));
        if (!note.getUser().getId().equals(userId)) {
            throw new UserNoteMismatchException(userId,noteId);
        }
        return note;
    }
}
