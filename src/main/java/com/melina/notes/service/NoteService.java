package com.melina.notes.service;

import com.melina.notes.dto.CreateUpdateNoteDto;
import com.melina.notes.dto.NoteDto;
import com.melina.notes.entity.Note;
import com.melina.notes.entity.Tag;
import com.melina.notes.exception.NoteNotFoundException;
import com.melina.notes.exception.UserNoteMismatchException;
import com.melina.notes.filter.NoteFilter;
import com.melina.notes.filter.NoteSpecification;
import com.melina.notes.mapper.NoteMapper;
import com.melina.notes.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    public NoteDto createNoteForUser(Long userId, CreateUpdateNoteDto noteDTO) {
        String title = resolveTitle(noteDTO.getTitle());

        Note note = Note.builder()
                .title(title)
                .content(noteDTO.getContent())
                .created(Instant.now())
                .updated(Instant.now())
                .tags(tagService.getOrCreateTags(noteDTO.getTagsRaw()))
                .user(userService.getUser(userId))
                .deleted(false)
                .archived(false)
                .pinned(false)
                .build();

        note = noteRepository.save(note);
        return noteMapper.toNoteDTO(note);
    }

    @Transactional(readOnly = true)
    public Page<NoteDto> getAllNotes(NoteFilter filter, Pageable pageable) {
        return noteRepository
                .findAll(NoteSpecification.build(filter), pageable)
                .map(noteMapper::toNoteDTO);
    }

    @Transactional(readOnly = true)
    public NoteDto getNoteById(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);
        return noteMapper.toNoteDTO(note);
    }

    @Transactional(readOnly = true)
    public CreateUpdateNoteDto getNoteByIdForView(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);

        CreateUpdateNoteDto editNote = noteMapper.toEditNoteDto(note);

        List<String> tagsAsStrings = note.getTags()
                .stream()
                .map(Tag::getTag)
                .toList();

        editNote.setTagsRaw(String.join(", ", tagsAsStrings));
        return editNote;
    }

    public NoteDto updateNoteForUser(Long userId, Long noteId, CreateUpdateNoteDto noteDTO) {
        Note note = getNote(userId, noteId);

        noteMapper.updateNote(note, noteDTO);
        note.setTitle(resolveTitle(note.getTitle()));
        note.setTags(tagService.getOrCreateTags(noteDTO.getTagsRaw()));
        note.setUpdated(Instant.now());

        note = noteRepository.save(note);

        tagService.deleteAllWithNoNotes();

        return noteMapper.toNoteDTO(note);
    }

    public void deleteNoteForUser(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);

        note.getTags().clear();
        noteRepository.delete(note);

        tagService.deleteAllWithNoNotes();
    }

    public NoteDto softDeleteNoteForUser(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);

        note.setDeleted(true);
        note.setArchived(false);
        note.setPinned(false);
        note.setUpdated(Instant.now());

        note = noteRepository.save(note);
        return noteMapper.toNoteDTO(note);
    }

    public NoteDto restoreNoteForUser(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);

        note.setDeleted(false);
        note.setArchived(false);
        note.setUpdated(Instant.now());

        note = noteRepository.save(note);
        return noteMapper.toNoteDTO(note);
    }

    public void emptyTrashForUser(Long userId) {
        List<Note> deletedNotes = noteRepository.findAllByUserIdAndDeleted(userId, true);

        for (Note note : deletedNotes) {
            note.getTags().clear();
        }

        noteRepository.deleteAll(deletedNotes);
        tagService.deleteAllWithNoNotes();
    }

    public NoteDto togglePinNoteForUser(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);

        note.setPinned(!note.isPinned());
        note.setUpdated(Instant.now());

        note = noteRepository.save(note);
        return noteMapper.toNoteDTO(note);
    }

    public NoteDto archiveNoteForUser(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);

        note.setArchived(true);
        note.setDeleted(false);
        note.setPinned(false);
        note.setUpdated(Instant.now());

        note = noteRepository.save(note);
        return noteMapper.toNoteDTO(note);
    }

    public NoteDto unarchiveNoteForUser(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);

        note.setArchived(false);
        note.setUpdated(Instant.now());

        note = noteRepository.save(note);
        return noteMapper.toNoteDTO(note);
    }

    private Note getNote(Long userId, Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        if (!note.getUser().getId().equals(userId)) {
            throw new UserNoteMismatchException(userId, noteId);
        }

        return note;
    }

    private String resolveTitle(String title) {
        if (title != null && !title.isBlank()) {
            return title.trim();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return "Notiz " + LocalDate.now().format(formatter);
    }
}