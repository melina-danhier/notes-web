package com.melina.notes.controller;

import com.melina.notes.dto.CreateNoteDto;
import com.melina.notes.dto.NoteDto;
import com.melina.notes.dto.SignupDto;
import com.melina.notes.filter.NoteFilter;
import com.melina.notes.security.CustomUserDetails;
import com.melina.notes.service.NoteService;
import com.melina.notes.service.TagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.ConcurrentModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PageControllerTest {

    @Mock
    private NoteService noteService;

    @Mock
    private TagService tagService;

    @InjectMocks
    private PageController pageController;

    @Test
    void login_withParameters_addsMessages() {
        ConcurrentModel model = new ConcurrentModel();

        String view = pageController.login(model, "true", "true", "true");

        assertAll(
                () -> assertEquals("login", view),
                () -> assertTrue(model.containsAttribute("error")),
                () -> assertTrue(model.containsAttribute("logout")),
                () -> assertTrue(model.containsAttribute("success"))
        );
    }

    @Test
    void signup_addsEmptySignupDto() {
        ConcurrentModel model = new ConcurrentModel();

        String view = pageController.signup(model, null);

        assertAll(
                () -> assertEquals("signup", view),
                () -> assertInstanceOf(SignupDto.class, model.getAttribute("signupDto"))
        );
    }

    @Test
    void notesPage_buildsFilterAndSplitsPinnedNotes() {
        CustomUserDetails user = userDetails();
        NoteDto pinned = NoteDto.builder().id(1L).pinned(true).build();
        NoteDto unpinned = NoteDto.builder().id(2L).pinned(false).build();
        when(noteService.getAllNotes(any(), any()))
                .thenReturn(new PageImpl<>(List.of(pinned, unpinned)));
        when(tagService.getAllTagsByUserId(user.getId())).thenReturn(List.of());
        ConcurrentModel model = new ConcurrentModel();

        String view = pageController.notesPage(
                model, user, 0, 6, "created", "desc", "work", "suche");

        ArgumentCaptor<NoteFilter> filterCaptor = ArgumentCaptor.forClass(NoteFilter.class);
        verify(noteService).getAllNotes(filterCaptor.capture(), any(Pageable.class));
        NoteFilter filter = filterCaptor.getValue();

        assertAll(
                () -> assertEquals("notes", view),
                () -> assertEquals(user.getId(), filter.getUserId()),
                () -> assertEquals("work", filter.getTagFilter()),
                () -> assertEquals("suche", filter.getSearchTerm()),
                () -> assertEquals(false, filter.getDeleted()),
                () -> assertEquals(false, filter.getArchived()),
                () -> assertEquals(List.of(pinned), model.getAttribute("pinnedNotes")),
                () -> assertEquals(List.of(unpinned), model.getAttribute("unpinnedNotes"))
        );
    }

    @Test
    void trashPage_filtersDeletedNotesAndDoesNotLoadTags() {
        CustomUserDetails user = userDetails();
        when(noteService.getAllNotes(any(), any())).thenReturn(new PageImpl<>(List.of()));
        ConcurrentModel model = new ConcurrentModel();

        String view = pageController.trashPage(model, user, 0, 6, "created", "desc");

        ArgumentCaptor<NoteFilter> filterCaptor = ArgumentCaptor.forClass(NoteFilter.class);
        verify(noteService).getAllNotes(filterCaptor.capture(), any(Pageable.class));

        assertEquals("trash", view);
        assertEquals(true, filterCaptor.getValue().getDeleted());
        verifyNoInteractions(tagService);
    }

    @Test
    void editNote_addsNoteAndJoinedTagsToModel() {
        CustomUserDetails user = userDetails();
        CreateNoteDto note = CreateNoteDto.builder()
                .title("Titel")
                .content("Inhalt")
                .tags(List.of("work", "home"))
                .build();
        when(noteService.getNoteByIdForView(user.getId(), 10L)).thenReturn(note);
        ConcurrentModel model = new ConcurrentModel();

        String view = pageController.editNote(10L, model, user);

        assertAll(
                () -> assertEquals("note-view", view),
                () -> assertSame(note, model.getAttribute("noteDto")),
                () -> assertEquals("work, home", model.getAttribute("tagsInputValue"))
        );
    }

    private CustomUserDetails userDetails() {
        return new CustomUserDetails(5L, "user@mail.com", "password", "ROLE_USER");
    }
}
