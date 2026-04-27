package com.melina.notes.service;

import com.melina.notes.dto.EditNoteDTO;
import com.melina.notes.dto.NoteDTO;
import com.melina.notes.entity.Note;
import com.melina.notes.entity.Tag;
import com.melina.notes.exception.NoteNotFoundException;
import com.melina.notes.exception.UserNoteMismatchException;
import com.melina.notes.filter.NoteFilter;
import com.melina.notes.filter.NoteSpecification;
import com.melina.notes.mapper.NoteMapper;
import com.melina.notes.repository.NoteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
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

    public Page<NoteDTO> getAllNotes(NoteFilter filter, PageRequest pageRequest) {
        return noteRepository
                .findAll(NoteSpecification.build(filter), pageRequest)
                .map(noteMapper::toNoteDTO);
    }

    public NoteDTO getNoteById(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);
        return noteMapper.toNoteDTO(note);
    }

    public EditNoteDTO getNoteByIdForView(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);
        EditNoteDTO editNote = noteMapper.toEditNoteDto(note);
        List<String> tagsAsStrings = note.getTags().stream().map(Tag::getTag).toList();
        editNote.setTagsRaw(String.join(", ", tagsAsStrings));
        return editNote;
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

    public NoteDTO softDeleteNoteForUser(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);
        note.setDeleted(true);
        note = noteRepository.save(note);
        return noteMapper.toNoteDTO(note);
    }

    public NoteDTO restoreNoteForUser(Long id, Long noteId) {
        Note note = getNote(id, noteId);
        note.setDeleted(false);
        note = noteRepository.save(note);
        return noteMapper.toNoteDTO(note);
    }

    public void emptyTrashForUser(Long userId) {
        noteRepository.deleteAllByUserIdAndDeleted(userId, true);
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
