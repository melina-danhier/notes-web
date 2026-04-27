package com.melina.notes.service;

import com.melina.notes.dto.EditNoteDTO;
import com.melina.notes.dto.NoteDTO;
import com.melina.notes.entity.Note;
import com.melina.notes.entity.Tag;
import com.melina.notes.entity.User;
import com.melina.notes.exception.NoteNotFoundException;
import com.melina.notes.exception.UserNoteMismatchException;
import com.melina.notes.mapper.NoteMapper;
import com.melina.notes.repository.NoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        EditNoteDTO input = new EditNoteDTO();
        input.setTitle("Titel");
        input.setContent("Inhalt");
        input.setTagsRaw("work");

        Tag tag = Tag.builder().id(10L).tag("work").build();
        User user = new User();
        user.setId(5L);

        Note savedNote = Note.builder()
                .id(100L)
                .title("Titel")
                .content("Inhalt")
                .tags(List.of(tag))
                .user(user)
                .build();

        NoteDTO mappedDto = new NoteDTO();
        mappedDto.setId(100L);

        when(tagService.getOrCreateTags("work")).thenReturn(List.of(tag));
        when(userService.getUser(5L)).thenReturn(user);
        when(noteRepository.save(any(Note.class))).thenReturn(savedNote);
        when(noteMapper.toNoteDTO(savedNote)).thenReturn(mappedDto);

        NoteDTO result = noteService.createNoteForUser(5L, input);

        assertSame(mappedDto, result);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());
        Note noteToSave = noteCaptor.getValue();

        assertEquals("Titel", noteToSave.getTitle());
        assertEquals("Inhalt", noteToSave.getContent());
        assertEquals(user, noteToSave.getUser());
        assertEquals(1, noteToSave.getTags().size());
        assertNotNull(noteToSave.getCreated());
        assertNotNull(noteToSave.getUpdated());
    }

    @Test
    void getNoteById_throwsWhenNoteDoesNotExist() {
        when(noteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class, () -> noteService.getNoteById(1L, 99L));
    }

    @Test
    void getNoteById_throwsWhenUserIsNotOwner() {
        User owner = new User();
        owner.setId(2L);

        Note note = Note.builder()
                .id(99L)
                .title("t")
                .user(owner)
                .build();

        when(noteRepository.findById(99L)).thenReturn(Optional.of(note));

        assertThrows(UserNoteMismatchException.class, () -> noteService.getNoteById(1L, 99L));
    }

    @Test
    void softDeleteNoteForUser_setsDeletedFlagToTrue() {
        User owner = new User();
        owner.setId(1L);

        Note note = Note.builder()
                .id(10L)
                .title("Titel")
                .deleted(false)
                .user(owner)
                .build();

        NoteDTO mappedDto = new NoteDTO();
        mappedDto.setId(10L);

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteMapper.toNoteDTO(any(Note.class))).thenReturn(mappedDto);

        NoteDTO result = noteService.softDeleteNoteForUser(1L, 10L);

        assertSame(mappedDto, result);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());
        assertTrue(noteCaptor.getValue().isDeleted());
    }

    @Test
    void restoreNoteForUser_setsDeletedFlagToFalse() {
        User owner = new User();
        owner.setId(1L);

        Note note = Note.builder()
                .id(10L)
                .title("Titel")
                .deleted(true)
                .user(owner)
                .build();

        NoteDTO mappedDto = new NoteDTO();
        mappedDto.setId(10L);

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noteMapper.toNoteDTO(any(Note.class))).thenReturn(mappedDto);

        NoteDTO result = noteService.restoreNoteForUser(1L, 10L);

        assertSame(mappedDto, result);

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());
        assertFalse(noteCaptor.getValue().isDeleted());
    }

    @Test
    void emptyTrashForUser_deletesOnlySoftDeletedNotes() {
        noteService.emptyTrashForUser(7L);

        verify(noteRepository).deleteAllByUserIdAndDeleted(7L, true);
    }
}

