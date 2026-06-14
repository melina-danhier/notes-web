package com.melina.notes.service;

import com.melina.notes.dto.CreateNoteDto;
import com.melina.notes.dto.NoteDto;
import com.melina.notes.entity.Note;
import com.melina.notes.exception.InvalidNoteStateException;
import com.melina.notes.exception.NoteDeletedException;
import com.melina.notes.exception.NoteNotFoundException;
import com.melina.notes.exception.NoteNotInTrashException;
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

    public NoteDto createNoteForUser(Long userId, CreateNoteDto noteDTO) {
        String title = resolveTitle(noteDTO.getTitle());

        Note note = Note.builder()
                .title(title)
                .content(noteDTO.getContent())
                .created(Instant.now())
                .updated(Instant.now())
                .tags(tagService.getOrCreateTags(noteDTO.getTags()))
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
    public CreateNoteDto getNoteByIdForView(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);
        return noteMapper.toEditNoteDto(note);
    }

    public NoteDto updateNoteForUser(Long userId, Long noteId, CreateNoteDto noteDTO) {
        Note note = getNote(userId, noteId);
        ensureNotDeleted(note, "bearbeitet");

        noteMapper.updateNote(note, noteDTO);
        note.setTitle(resolveTitle(note.getTitle()));
        note.setTags(tagService.getOrCreateTags(noteDTO.getTags()));
        note.setUpdated(Instant.now());

        note = noteRepository.save(note);

        tagService.deleteAllWithNoNotes();

        return noteMapper.toNoteDTO(note);
    }

    public void deleteNoteForUser(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);
        if (!note.isDeleted()) {
            throw new NoteNotInTrashException(noteId);
        }

        note.getTags().clear();
        noteRepository.delete(note);

        tagService.deleteAllWithNoNotes();
    }

    public NoteDto softDeleteNoteForUser(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);
        if (note.isDeleted()) {
            throw new InvalidNoteStateException(noteId, "Die Notiz befindet sich bereits im Papierkorb");
        }

        note.setDeleted(true);
        note.setArchived(false);
        note.setPinned(false);
        note.setUpdated(Instant.now());

        note = noteRepository.save(note);
        return noteMapper.toNoteDTO(note);
    }

    public NoteDto restoreNoteForUser(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);
        if (!note.isDeleted()) {
            throw new InvalidNoteStateException(noteId, "Nur Notizen im Papierkorb können wiederhergestellt werden");
        }

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
        ensureNotDeleted(note, "angepinnt");
        if (note.isArchived()) {
            throw new InvalidNoteStateException(noteId, "Archivierte Notizen können nicht angepinnt werden");
        }

        note.setPinned(!note.isPinned());
        note.setUpdated(Instant.now());

        note = noteRepository.save(note);
        return noteMapper.toNoteDTO(note);
    }

    public NoteDto archiveNoteForUser(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);
        ensureNotDeleted(note, "archiviert");
        if (note.isArchived()) {
            throw new InvalidNoteStateException(noteId, "Die Notiz ist bereits archiviert");
        }

        note.setArchived(true);
        note.setDeleted(false);
        note.setPinned(false);
        note.setUpdated(Instant.now());

        note = noteRepository.save(note);
        return noteMapper.toNoteDTO(note);
    }

    public NoteDto unarchiveNoteForUser(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);
        ensureNotDeleted(note, "aus dem Archiv entfernt");
        if (!note.isArchived()) {
            throw new InvalidNoteStateException(noteId, "Die Notiz ist nicht archiviert");
        }

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

    private void ensureNotDeleted(Note note, String action) {
        if (note.isDeleted()) {
            throw new NoteDeletedException(note.getId(), action);
        }
    }

    private String resolveTitle(String title) {
        if (title != null && !title.isBlank()) {
            return title.trim();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return "Notiz " + LocalDate.now().format(formatter);
    }
}
