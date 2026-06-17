package com.melina.notes.service;

import com.melina.notes.dto.NoteFormDto;
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

    public NoteDto createNoteForUser(Long userId, NoteFormDto noteForm) {
        String title = resolveTitle(noteForm.getTitle());

        Note note = Note.builder()
                .title(title)
                .content(noteForm.getContent())
                .created(Instant.now())
                .updated(Instant.now())
                .tags(tagService.getOrCreateTags(noteForm.getTags()))
                .user(userService.getUser(userId))
                .deleted(false)
                .archived(false)
                .pinned(false)
                .build();

        note = noteRepository.save(note);
        return noteMapper.toNoteDto(note);
    }

    @Transactional(readOnly = true)
    public Page<NoteDto> getAllNotes(NoteFilter filter, Pageable pageable) {
        return noteRepository
                .findAll(NoteSpecification.build(filter), pageable)
                .map(noteMapper::toNoteDto);
    }

    @Transactional(readOnly = true)
    public NoteDto getNoteById(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);
        return noteMapper.toNoteDto(note);
    }

    @Transactional(readOnly = true)
    public NoteFormDto getNoteByIdForView(Long userId, Long noteId) {
        Note note = getNote(userId, noteId);
        return noteMapper.toNoteFormDto(note);
    }

    public NoteDto updateNoteForUser(Long userId, Long noteId, NoteFormDto noteForm) {
        Note note = getNote(userId, noteId);
        ensureNotDeleted(note, "bearbeitet");

        noteMapper.updateNote(note, noteForm);
        note.setTitle(resolveTitle(note.getTitle()));
        note.setTags(tagService.getOrCreateTags(noteForm.getTags()));
        note.setUpdated(Instant.now());

        note = noteRepository.save(note);

        tagService.deleteAllWithNoNotes();

        return noteMapper.toNoteDto(note);
    }

    public NoteDto toggleTaskForUser(Long userId, Long noteId, int taskIndex) {
        Note note = getNote(userId, noteId);
        ensureNotDeleted(note, "bearbeitet");

        String content = note.getContent() == null ? "" : note.getContent();
        String[] lines = content.split("\n", -1);
        int currentTaskIndex = 0;
        boolean toggled = false;

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            if (!lines[lineIndex].matches("\\s*\\[[ xX]]\\s+.+?(?:;\\s*)?")) {
                continue;
            }
            if (currentTaskIndex++ != taskIndex) {
                continue;
            }

            lines[lineIndex] = lines[lineIndex].replaceFirst(
                    "\\[([ xX])]","[x]".equalsIgnoreCase(lines[lineIndex].trim().substring(0, 3))
                            ? "[ ]"
                            : "[x]"
            );
            toggled = true;
            break;
        }

        if (!toggled) {
            throw new InvalidNoteStateException(noteId, "Aufgabe wurde nicht gefunden");
        }

        note.setContent(String.join("\n", lines));
        note.setUpdated(Instant.now());
        note = noteRepository.save(note);
        return noteMapper.toNoteDto(note);
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
        return noteMapper.toNoteDto(note);
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
        return noteMapper.toNoteDto(note);
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
        return noteMapper.toNoteDto(note);
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
        return noteMapper.toNoteDto(note);
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
        return noteMapper.toNoteDto(note);
    }

    private Note getNote(Long userId, Long noteId) {
        return noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));
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
