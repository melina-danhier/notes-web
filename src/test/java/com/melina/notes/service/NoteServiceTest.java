package com.melina.notes.service;

import com.melina.notes.dto.CreateNoteDto;
import com.melina.notes.dto.NoteDto;
import com.melina.notes.entity.Note;
import com.melina.notes.entity.Tag;
import com.melina.notes.entity.User;
import com.melina.notes.exception.NoteNotFoundException;
import com.melina.notes.exception.UserNoteMismatchException;

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

import static org.mockito.ArgumentMatchers.eq;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        CreateNoteDto input = createInput("Titel", "Inhalt", List.of("work"));

        Tag tag = createTag(10L, "work");
        User user = createUser(5L);
        Note savedNote = createNote(100L, user);
        NoteDto mappedDto = createNoteDto(100L);

        when(tagService.getOrCreateTags(List.of("work"))).thenReturn(List.of(tag));
        when(userService.getUser(5L)).thenReturn(user);
        when(noteRepository.save(any(Note.class))).thenReturn(savedNote);
        when(noteMapper.toNoteDTO(savedNote)).thenReturn(mappedDto);

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
        verify(noteMapper).toNoteDTO(savedNote);
    }

    @Test
    void createNoteForUser_blankTitle_usesDefaultTitle() {
        CreateNoteDto input = createInput("   ", "Inhalt", List.of());

        User user = createUser(5L);
        Note savedNote = createNote(100L, user);
        NoteDto mappedDto = createNoteDto(100L);

        when(tagService.getOrCreateTags(List.of())).thenReturn(List.of());
        when(userService.getUser(5L)).thenReturn(user);
        when(noteRepository.save(any(Note.class))).thenReturn(savedNote);
        when(noteMapper.toNoteDTO(savedNote)).thenReturn(mappedDto);

        noteService.createNoteForUser(5L, input);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());

        assertTrue(noteCaptor.getValue().getTitle().startsWith("Notiz "));
    }

    @Test
    void createNoteForUser_titleWithSpaces_trimsTitle() {
        CreateNoteDto input = createInput("  Titel  ", "Inhalt", List.of());

        User user = createUser(5L);
        Note savedNote = createNote(100L, user);
        NoteDto mappedDto = createNoteDto(100L);

        when(tagService.getOrCreateTags(List.of())).thenReturn(List.of());
        when(userService.getUser(5L)).thenReturn(user);
        when(noteRepository.save(any(Note.class))).thenReturn(savedNote);
        when(noteMapper.toNoteDTO(savedNote)).thenReturn(mappedDto);

        noteService.createNoteForUser(5L, input);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());

        assertEquals("Titel", noteCaptor.getValue().getTitle());
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

        when(noteMapper.toNoteDTO(note)).thenReturn(dto);

        Page<NoteDto> result = noteService.getAllNotes(filter, pageRequest);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertSame(dto, result.getContent().getFirst());

        verify(noteRepository).findAll(
                ArgumentMatchers.<Specification<Note>>any(),
                eq(pageRequest)
        );
        verify(noteMapper).toNoteDTO(note);
    }

    @Test
    void getNoteById_existingNoteOfUser_returnsMappedDto() {
        User owner = createUser(1L);
        Note note = createNote(10L, owner);
        NoteDto mappedDto = createNoteDto(10L);

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(noteMapper.toNoteDTO(note)).thenReturn(mappedDto);

        NoteDto result = noteService.getNoteById(1L, 10L);

        assertSame(mappedDto, result);
        verify(noteRepository).findById(10L);
        verify(noteMapper).toNoteDTO(note);
    }

    @Test
    void getNoteById_throwsWhenNoteDoesNotExist() {
        when(noteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class,
                () -> noteService.getNoteById(1L, 99L));

        verify(noteRepository).findById(99L);
        verifyNoInteractions(noteMapper);
    }

    @Test
    void getNoteById_throwsWhenUserIsNotOwner() {
        User owner = createUser(2L);
        Note note = createNote(99L, owner);

        when(noteRepository.findById(99L)).thenReturn(Optional.of(note));

        assertThrows(UserNoteMismatchException.class,
                () -> noteService.getNoteById(1L, 99L));

        verify(noteRepository).findById(99L);
        verifyNoInteractions(noteMapper);
    }

    @Test
    void getNoteByIdForView_returnsMappedEditDto() {
        User owner = createUser(1L);

        Note note = createNote(10L, owner);
        note.setTags(new ArrayList<>(List.of(
                createTag(1L, "work"),
                createTag(2L, "home")
        )));

        CreateNoteDto editDto = createInput("Titel", "Inhalt", List.of("work", "home"));

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(noteMapper.toEditNoteDto(note)).thenReturn(editDto);

        CreateNoteDto result = noteService.getNoteByIdForView(1L, 10L);

        assertSame(editDto, result);
        assertEquals(List.of("work", "home"), result.getTags());

        verify(noteRepository).findById(10L);
        verify(noteMapper).toEditNoteDto(note);
    }

    @Test
    void updateNoteForUser_updatesFieldsTagsAndDeletesUnusedTags() {
        User owner = createUser(1L);
        Note note = createNote(10L, owner);
        note.setTitle("Alt");
        note.setContent("Alt Content");
        note.setTags(new ArrayList<>(List.of(createTag(1L, "old"))));

        CreateNoteDto input = createInput("Neu", "Neuer Content", List.of("work"));
        Tag newTag = createTag(2L, "work");
        NoteDto mappedDto = createNoteDto(10L);

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));

        doAnswer(invocation -> {
            Note target = invocation.getArgument(0);
            CreateNoteDto dto = invocation.getArgument(1);
            target.setTitle(dto.getTitle());
            target.setContent(dto.getContent());
            return null;
        }).when(noteMapper).updateNote(note, input);

        when(tagService.getOrCreateTags(List.of("work"))).thenReturn(List.of(newTag));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteMapper.toNoteDTO(any(Note.class))).thenReturn(mappedDto);

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
    void updateNoteForUser_blankTitle_usesDefaultTitle() {
        User owner = createUser(1L);
        Note note = createNote(10L, owner);

        CreateNoteDto input = createInput("   ", "Neuer Content", List.of());
        NoteDto mappedDto = createNoteDto(10L);

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));

        doAnswer(invocation -> {
            Note target = invocation.getArgument(0);
            CreateNoteDto dto = invocation.getArgument(1);
            target.setTitle(dto.getTitle());
            target.setContent(dto.getContent());
            return null;
        }).when(noteMapper).updateNote(note, input);

        when(tagService.getOrCreateTags(List.of())).thenReturn(List.of());
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteMapper.toNoteDTO(any(Note.class))).thenReturn(mappedDto);

        noteService.updateNoteForUser(1L, 10L, input);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());

        assertTrue(noteCaptor.getValue().getTitle().startsWith("Notiz "));
    }

    @Test
    void deleteNoteForUser_clearsTagsDeletesNoteAndDeletesUnusedTags() {
        User owner = createUser(1L);
        Note note = createNote(10L, owner);
        note.setTags(new ArrayList<>(List.of(
                createTag(1L, "work"),
                createTag(2L, "home")
        )));

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));

        noteService.deleteNoteForUser(1L, 10L);

        assertTrue(note.getTags().isEmpty());

        verify(noteRepository).delete(note);
        verify(tagService).deleteAllWithNoNotes();
    }

    @Test
    void softDeleteNoteForUser_setsDeletedFlagToTrueAndClearsArchiveAndPin() {
        User owner = createUser(1L);

        Note note = createNote(10L, owner);
        note.setDeleted(false);
        note.setArchived(true);
        note.setPinned(true);

        NoteDto mappedDto = createNoteDto(10L);

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteMapper.toNoteDTO(any(Note.class))).thenReturn(mappedDto);

        NoteDto result = noteService.softDeleteNoteForUser(1L, 10L);

        assertSame(mappedDto, result);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());

        Note savedNote = noteCaptor.getValue();

        assertAll(
                () -> assertTrue(savedNote.isDeleted()),
                () -> assertFalse(savedNote.isArchived()),
                () -> assertFalse(savedNote.isPinned()),
                () -> assertNotNull(savedNote.getUpdated())
        );
    }

    @Test
    void restoreNoteForUser_setsDeletedAndArchivedToFalse() {
        User owner = createUser(1L);

        Note note = createNote(10L, owner);
        note.setDeleted(true);
        note.setArchived(true);

        NoteDto mappedDto = createNoteDto(10L);

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteMapper.toNoteDTO(any(Note.class))).thenReturn(mappedDto);

        NoteDto result = noteService.restoreNoteForUser(1L, 10L);

        assertSame(mappedDto, result);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());

        Note savedNote = noteCaptor.getValue();

        assertAll(
                () -> assertFalse(savedNote.isDeleted()),
                () -> assertFalse(savedNote.isArchived()),
                () -> assertNotNull(savedNote.getUpdated())
        );
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

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteMapper.toNoteDTO(any(Note.class))).thenReturn(mappedDto);

        NoteDto result = noteService.togglePinNoteForUser(1L, 10L);

        assertSame(mappedDto, result);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());

        Note savedNote = noteCaptor.getValue();

        assertAll(
                () -> assertTrue(savedNote.isPinned()),
                () -> assertNotNull(savedNote.getUpdated())
        );
    }

    @Test
    void archiveNoteForUser_setsArchivedFlagToTrueAndClearsDeletedAndPin() {
        User owner = createUser(1L);

        Note note = createNote(10L, owner);
        note.setDeleted(true);
        note.setArchived(false);
        note.setPinned(true);

        NoteDto mappedDto = createNoteDto(10L);

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteMapper.toNoteDTO(any(Note.class))).thenReturn(mappedDto);

        NoteDto result = noteService.archiveNoteForUser(1L, 10L);

        assertSame(mappedDto, result);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());

        Note savedNote = noteCaptor.getValue();

        assertAll(
                () -> assertTrue(savedNote.isArchived()),
                () -> assertFalse(savedNote.isDeleted()),
                () -> assertFalse(savedNote.isPinned()),
                () -> assertNotNull(savedNote.getUpdated())
        );
    }

    @Test
    void unarchiveNoteForUser_setsArchivedFlagToFalse() {
        User owner = createUser(1L);

        Note note = createNote(10L, owner);
        note.setArchived(true);

        NoteDto mappedDto = createNoteDto(10L);

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteMapper.toNoteDTO(any(Note.class))).thenReturn(mappedDto);

        NoteDto result = noteService.unarchiveNoteForUser(1L, 10L);

        assertSame(mappedDto, result);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());

        Note savedNote = noteCaptor.getValue();

        assertAll(
                () -> assertFalse(savedNote.isArchived()),
                () -> assertNotNull(savedNote.getUpdated())
        );
    }

    private CreateNoteDto createInput(String title, String content, List<String> tags) {
        CreateNoteDto dto = new CreateNoteDto();
        dto.setTitle(title);
        dto.setContent(content);
        dto.setTags(tags);
        return dto;
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
