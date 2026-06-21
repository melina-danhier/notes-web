package com.melina.notes.service;

import com.melina.notes.dto.NoteFormDto;
import com.melina.notes.dto.NoteDto;
import com.melina.notes.entity.Note;
import com.melina.notes.entity.Tag;
import com.melina.notes.entity.User;
import com.melina.notes.exception.InvalidNoteStateException;
import com.melina.notes.exception.NoteDeletedException;
import com.melina.notes.exception.NoteNotFoundException;
import com.melina.notes.exception.NoteNotInTrashException;

import com.melina.notes.filter.NoteFilter;
import com.melina.notes.mapper.NoteMapper;
import com.melina.notes.repository.NoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private NoteMapper noteMapper;

    @Mock
    private TagService tagService;

    @Mock
    private UserService userService;

    @InjectMocks
    private NoteService noteService;

    @Test
    void createNoteForUser_savesNewNoteAndReturnsMappedDto() {
        NoteFormDto input = createInput("Titel", "Inhalt", List.of("work"));

        Tag tag = createTag(10L, "work");
        User user = createUser(5L);
        Note savedNote = createNote(100L, user);
        NoteDto mappedDto = createNoteDto(100L);

        when(tagService.getOrCreateTags(List.of("work"))).thenReturn(List.of(tag));
        when(userService.getUser(5L)).thenReturn(user);
        when(noteRepository.save(any(Note.class))).thenReturn(savedNote);
        when(noteMapper.toNoteDto(savedNote)).thenReturn(mappedDto);

        NoteDto result = noteService.createNoteForUser(5L, input);

        assertSame(mappedDto, result);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());

        Note noteToSave = noteCaptor.getValue();

        assertAll(
                () -> assertEquals("Titel", noteToSave.getTitle()),
                () -> assertEquals("Inhalt", noteToSave.getContent()),
                () -> assertEquals(user, noteToSave.getUser()),
                () -> assertEquals(1, noteToSave.getTags().size()),
                () -> assertEquals("work", noteToSave.getTags().getFirst().getTag()),
                () -> assertFalse(noteToSave.isDeleted()),
                () -> assertFalse(noteToSave.isArchived()),
                () -> assertFalse(noteToSave.isPinned()),
                () -> assertNotNull(noteToSave.getCreated()),
                () -> assertNotNull(noteToSave.getUpdated())
        );

        verify(tagService).getOrCreateTags(List.of("work"));
        verify(userService).getUser(5L);
        verify(noteMapper).toNoteDto(savedNote);
    }

    @Test
    void getAllNotes_returnsMappedPage() {
        NoteFilter filter = NoteFilter.builder()
                .userId(1L)
                .deleted(false)
                .build();

        PageRequest pageRequest = PageRequest.of(0, 10);

        User owner = createUser(1L);
        Note note = createNote(10L, owner);
        NoteDto dto = createNoteDto(10L);

        Page<Note> notePage = new PageImpl<>(List.of(note), pageRequest, 1);

        when(noteRepository.findAll(
                ArgumentMatchers.<Specification<Note>>any(),
                eq(pageRequest)
        )).thenReturn(notePage);

        when(noteMapper.toNoteDto(note)).thenReturn(dto);

        Page<NoteDto> result = noteService.getAllNotes(filter, pageRequest);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertSame(dto, result.getContent().getFirst());

        verify(noteRepository).findAll(
                ArgumentMatchers.<Specification<Note>>any(),
                eq(pageRequest)
        );
        verify(noteMapper).toNoteDto(note);
    }

    @Test
    void getNoteById_existingNoteOfUser_returnsMappedDto() {
        User owner = createUser(1L);
        Note note = createNote(10L, owner);
        NoteDto mappedDto = createNoteDto(10L);

        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(note));
        when(noteMapper.toNoteDto(note)).thenReturn(mappedDto);

        NoteDto result = noteService.getNoteById(1L, 10L);

        assertSame(mappedDto, result);
        verify(noteRepository).findByIdAndUserId(10L, 1L);
        verify(noteMapper).toNoteDto(note);
    }

    @Test
    void getNoteById_throwsWhenNoteDoesNotExist() {
        when(noteRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class,
                () -> noteService.getNoteById(1L, 99L));

        verify(noteRepository).findByIdAndUserId(99L, 1L);
        verifyNoInteractions(noteMapper);
    }

    @Test
    void getNoteById_throwsNotFoundWhenUserIsNotOwner() {
        when(noteRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class,
                () -> noteService.getNoteById(1L, 99L));

        verify(noteRepository).findByIdAndUserId(99L, 1L);
        verifyNoInteractions(noteMapper);
    }

    @Test
    void updateNoteForUser_updatesFieldsTagsAndDeletesUnusedTags() {
        User owner = createUser(1L);
        Note note = createNote(10L, owner);
        note.setTitle("Alt");
        note.setContent("Alt Content");
        note.setTags(new ArrayList<>(List.of(createTag(1L, "old"))));

        NoteFormDto input = createInput("Neu", "Neuer Content", List.of("work"));
        Tag newTag = createTag(2L, "work");
        NoteDto mappedDto = createNoteDto(10L);

        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(note));

        doAnswer(invocation -> {
            Note target = invocation.getArgument(0);
            NoteFormDto dto = invocation.getArgument(1);
            target.setTitle(dto.title());
            target.setContent(dto.content());
            return null;
        }).when(noteMapper).updateNote(note, input);

        when(tagService.getOrCreateTags(List.of("work"))).thenReturn(List.of(newTag));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteMapper.toNoteDto(any(Note.class))).thenReturn(mappedDto);

        NoteDto result = noteService.updateNoteForUser(1L, 10L, input);

        assertSame(mappedDto, result);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());

        Note savedNote = noteCaptor.getValue();

        assertAll(
                () -> assertEquals("Neu", savedNote.getTitle()),
                () -> assertEquals("Neuer Content", savedNote.getContent()),
                () -> assertEquals(1, savedNote.getTags().size()),
                () -> assertEquals("work", savedNote.getTags().getFirst().getTag()),
                () -> assertNotNull(savedNote.getUpdated())
        );

        verify(tagService).deleteAllWithNoNotes();
    }

    @Test
    void deleteNoteForUser_clearsTagsDeletesNoteAndDeletesUnusedTags() {
        User owner = createUser(1L);
        Note note = createNote(10L, owner);
        note.setDeleted(true);
        note.setTags(new ArrayList<>(List.of(
                createTag(1L, "work"),
                createTag(2L, "home")
        )));

        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(note));

        noteService.deleteNoteForUser(1L, 10L);

        assertTrue(note.getTags().isEmpty());

        verify(noteRepository).delete(note);
        verify(tagService).deleteAllWithNoNotes();
    }

    @Test
    void toggleTaskForUser_updatesOnlySelectedTask() {
        User owner = createUser(1L);
        Note note = createNote(10L, owner);
        note.setContent("- [ ] Erste\nText\n- [x] Zweite");
        NoteDto mappedDto = createNoteDto(10L);

        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(note)).thenReturn(note);
        when(noteMapper.toNoteDto(note)).thenReturn(mappedDto);

        NoteDto result = noteService.toggleTaskForUser(1L, 10L, 1);

        assertSame(mappedDto, result);
        assertEquals("- [ ] Erste\nText\n- [ ] Zweite", note.getContent());
        assertNotNull(note.getUpdated());
        verify(noteRepository).save(note);
        verify(noteMapper).toNoteDto(note);
        verifyNoInteractions(tagService);
    }

    @Test
    void toggleTaskForUser_rejectsUnknownTaskIndex() {
        User owner = createUser(1L);
        Note note = createNote(10L, owner);
        note.setContent("- [ ] Aufgabe");
        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(note));

        assertThrows(InvalidNoteStateException.class,
                () -> noteService.toggleTaskForUser(1L, 10L, 2));

        verify(noteRepository, never()).save(any(Note.class));
    }

    @Test
    void deleteNoteForUser_noteOutsideTrash_throwsException() {
        Note note = createNote(10L, createUser(1L));
        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(note));

        assertThrows(NoteNotInTrashException.class,
                () -> noteService.deleteNoteForUser(1L, 10L));

        verify(noteRepository, never()).delete(any(Note.class));
        verifyNoInteractions(tagService);
    }

    @Test
    void softDeleteNoteForUser_setsDeletedFlagToTrueAndClearsArchiveAndPin() {
        User owner = createUser(1L);

        Note note = createNote(10L, owner);
        note.setDeleted(false);
        note.setArchived(true);
        note.setPinned(true);

        NoteDto mappedDto = createNoteDto(10L);

        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteMapper.toNoteDto(any(Note.class))).thenReturn(mappedDto);

        NoteDto result = noteService.softDeleteNoteForUser(1L, 10L);

        assertSame(mappedDto, result);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());

        Note savedNote = noteCaptor.getValue();

        assertAll(
                () -> assertTrue(savedNote.isDeleted()),
                () -> assertFalse(savedNote.isArchived()),
                () -> assertFalse(savedNote.isPinned())
        );
    }

    @Test
    void restoreNoteForUser_setsDeletedToFalse() {
        User owner = createUser(1L);

        Note note = createNote(10L, owner);
        note.setDeleted(true);

        NoteDto mappedDto = createNoteDto(10L);

        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteMapper.toNoteDto(any(Note.class))).thenReturn(mappedDto);

        NoteDto result = noteService.restoreNoteForUser(1L, 10L);

        assertSame(mappedDto, result);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());

        Note savedNote = noteCaptor.getValue();

        assertFalse(savedNote.isDeleted());
    }

    @Test
    void restoreNoteForUser_activeNote_throwsException() {
        Note note = createNote(10L, createUser(1L));
        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(note));

        assertThrows(InvalidNoteStateException.class,
                () -> noteService.restoreNoteForUser(1L, 10L));

        verify(noteRepository, never()).save(any());
    }

    @Test
    void updateNoteForUser_deletedNote_throwsException() {
        Note note = createNote(10L, createUser(1L));
        note.setDeleted(true);
        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(note));

        assertThrows(NoteDeletedException.class,
                () -> noteService.updateNoteForUser(
                        1L, 10L, createInput("Titel", "Inhalt", List.of())));

        verifyNoInteractions(noteMapper, tagService);
        verify(noteRepository, never()).save(any());
    }

    @Test
    void emptyTrashForUser_deletesSoftDeletedNotesAndUnusedTags() {
        User owner = createUser(7L);

        Note deletedNote1 = createNote(1L, owner);
        deletedNote1.setDeleted(true);
        deletedNote1.setTags(new ArrayList<>(List.of(createTag(1L, "work"))));

        Note deletedNote2 = createNote(2L, owner);
        deletedNote2.setDeleted(true);
        deletedNote2.setTags(new ArrayList<>(List.of(createTag(2L, "home"))));

        List<Note> deletedNotes = List.of(deletedNote1, deletedNote2);

        when(noteRepository.findAllByUserIdAndDeleted(7L, true)).thenReturn(deletedNotes);

        noteService.emptyTrashForUser(7L);

        assertTrue(deletedNote1.getTags().isEmpty());
        assertTrue(deletedNote2.getTags().isEmpty());

        verify(noteRepository).findAllByUserIdAndDeleted(7L, true);
        verify(noteRepository).deleteAll(deletedNotes);
        verify(tagService).deleteAllWithNoNotes();
    }

    @Test
    void togglePinNoteForUser_togglesPinnedFlag() {
        User owner = createUser(1L);

        Note note = createNote(10L, owner);
        note.setPinned(false);

        NoteDto mappedDto = createNoteDto(10L);

        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteMapper.toNoteDto(any(Note.class))).thenReturn(mappedDto);

        NoteDto result = noteService.togglePinNoteForUser(1L, 10L);

        assertSame(mappedDto, result);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());

        Note savedNote = noteCaptor.getValue();

        assertTrue(savedNote.isPinned());
    }

    @Test
    void togglePinNoteForUser_archivedNote_throwsException() {
        Note note = createNote(10L, createUser(1L));
        note.setArchived(true);
        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(note));

        assertThrows(InvalidNoteStateException.class,
                () -> noteService.togglePinNoteForUser(1L, 10L));

        verify(noteRepository, never()).save(any());
    }

    @Test
    void archiveNoteForUser_setsArchivedFlagToTrueAndClearsPinned() {
        User owner = createUser(1L);

        Note note = createNote(10L, owner);
        note.setArchived(false);
        note.setPinned(true);

        NoteDto mappedDto = createNoteDto(10L);

        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteMapper.toNoteDto(any(Note.class))).thenReturn(mappedDto);

        NoteDto result = noteService.archiveNoteForUser(1L, 10L);

        assertSame(mappedDto, result);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());

        Note savedNote = noteCaptor.getValue();

        assertAll(
                () -> assertTrue(savedNote.isArchived()),
                () -> assertFalse(savedNote.isPinned())
        );
    }

    @Test
    void unarchiveNoteForUser_setsArchivedFlagToFalse() {
        User owner = createUser(1L);

        Note note = createNote(10L, owner);
        note.setArchived(true);

        NoteDto mappedDto = createNoteDto(10L);

        when(noteRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteMapper.toNoteDto(any(Note.class))).thenReturn(mappedDto);

        NoteDto result = noteService.unarchiveNoteForUser(1L, 10L);

        assertSame(mappedDto, result);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());

        Note savedNote = noteCaptor.getValue();

        assertFalse(savedNote.isArchived());
    }

    private NoteFormDto createInput(String title, String content, List<String> tags) {
        return NoteFormDto.builder()
                .title(title)
                .content(content)
                .tags(tags)
                .build();
    }

    private NoteDto createNoteDto(Long id) {
        return NoteDto.builder()
                .id(id)
                .build();
    }

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Tag createTag(Long id, String tagName) {
        return Tag.builder()
                .id(id)
                .tag(tagName)
                .build();
    }

    private Note createNote(Long id, User user) {
        return Note.builder()
                .id(id)
                .title("Titel")
                .content("Inhalt")
                .created(Instant.now())
                .updated(Instant.now())
                .deleted(false)
                .archived(false)
                .pinned(false)
                .tags(new ArrayList<>())
                .user(user)
                .build();
    }
}
