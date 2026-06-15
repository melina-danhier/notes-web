package com.melina.notes.controller;

import com.melina.notes.dto.NoteFormDto;
import com.melina.notes.dto.NoteDto;
import com.melina.notes.dto.NotePageData;
import com.melina.notes.dto.NotePageRequest;
import com.melina.notes.dto.SignupDto;
import com.melina.notes.security.CustomUserDetails;
import com.melina.notes.service.NotePageService;
import com.melina.notes.service.NoteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.ui.ConcurrentModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageControllerTest {

    @Mock
    private NoteService noteService;

    @Mock
    private NotePageService notePageService;

    @InjectMocks
    private PageController pageController;

    @Test
    void login_returnsLoginView() {
        assertEquals("login", pageController.login());
    }

    @Test
    void signup_addsEmptySignupDto() {
        ConcurrentModel model = new ConcurrentModel();

        String view = pageController.signup(model);

        assertAll(
                () -> assertEquals("signup", view),
                () -> assertInstanceOf(SignupDto.class, model.getAttribute("signupDto"))
        );
    }

    @Test
    void newNote_addsEmptyNoteAndNullIdToModel() {
        ConcurrentModel model = new ConcurrentModel();

        String view = pageController.newNote(model);

        assertAll(
                () -> assertEquals("note-view", view),
                () -> assertInstanceOf(NoteFormDto.class, model.getAttribute("noteDto")),
                () -> assertNull(model.getAttribute("noteId")),
                () -> assertEquals("", model.getAttribute("tagsInputValue"))
        );
    }

    @Test
    void notesPage_delegatesAndAddsPageDataToModel() {
        CustomUserDetails user = userDetails();
        NotePageRequest request = new NotePageRequest();
        NoteDto pinned = NoteDto.builder().id(1L).pinned(true).build();
        NotePageData pageData = pageData(List.of(pinned), List.of());
        when(notePageService.getActiveNotes(user.getId(), request)).thenReturn(pageData);
        ConcurrentModel model = new ConcurrentModel();

        String view = pageController.notesPage(model, user, request);

        assertAll(
                () -> assertEquals("notes", view),
                () -> assertEquals(List.of(pinned), model.getAttribute("pinnedNotes")),
                () -> assertEquals("created", model.getAttribute("sortField")),
                () -> assertEquals(user.getDisplayName(), model.getAttribute("displayname"))
        );
        verify(notePageService).getActiveNotes(user.getId(), request);
    }

    @Test
    void trashPage_delegatesToDeletedNotes() {
        CustomUserDetails user = userDetails();
        NotePageRequest request = new NotePageRequest();
        NotePageData pageData = pageData(List.of(), List.of());
        when(notePageService.getDeletedNotes(user.getId(), request)).thenReturn(pageData);
        ConcurrentModel model = new ConcurrentModel();

        String view = pageController.trashPage(model, user, request);

        assertEquals("trash", view);
        verify(notePageService).getDeletedNotes(user.getId(), request);
    }

    @Test
    void editNote_addsNoteAndJoinedTagsToModel() {
        CustomUserDetails user = userDetails();
        NoteFormDto note = NoteFormDto.builder()
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
                () -> assertEquals(10L, model.getAttribute("noteId")),
                () -> assertEquals("work, home", model.getAttribute("tagsInputValue"))
        );
    }

    private NotePageData pageData(List<NoteDto> pinnedNotes, List<NoteDto> unpinnedNotes) {
        List<NoteDto> notes = new java.util.ArrayList<>(pinnedNotes);
        notes.addAll(unpinnedNotes);

        return NotePageData.builder()
                .notes(new PageImpl<>(notes))
                .pinnedNotes(pinnedNotes)
                .unpinnedNotes(unpinnedNotes)
                .allTags(List.of())
                .currentPage(0)
                .pageSize(6)
                .sortField("created")
                .sortDir("desc")
                .build();
    }

    private CustomUserDetails userDetails() {
        return new CustomUserDetails(5L, "user@mail.com", "password", "ROLE_USER");
    }
}
