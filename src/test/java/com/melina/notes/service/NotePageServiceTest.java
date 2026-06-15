package com.melina.notes.service;

import com.melina.notes.dto.NoteDto;
import com.melina.notes.dto.NotePageData;
import com.melina.notes.dto.NotePageRequest;
import com.melina.notes.filter.NoteFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotePageServiceTest {

    @Mock
    private NoteService noteService;

    @Mock
    private TagService tagService;

    @InjectMocks
    private NotePageService notePageService;

    @Test
    void getActiveNotes_buildsFilterAndPreparesViewData() {
        NotePageRequest request = new NotePageRequest();
        request.setTagFilter("work");
        request.setSearch("meeting");
        NoteDto pinned = NoteDto.builder().id(1L).pinned(true).build();
        NoteDto unpinned = NoteDto.builder().id(2L).pinned(false).build();
        when(noteService.getAllNotes(any(), any()))
                .thenReturn(new PageImpl<>(List.of(pinned, unpinned)));
        when(tagService.getAllTagsByUserId(5L)).thenReturn(List.of("work"));

        NotePageData result = notePageService.getActiveNotes(5L, request);

        ArgumentCaptor<NoteFilter> filterCaptor = ArgumentCaptor.forClass(NoteFilter.class);
        verify(noteService).getAllNotes(filterCaptor.capture(), any(Pageable.class));
        NoteFilter filter = filterCaptor.getValue();

        assertAll(
                () -> assertEquals(5L, filter.getUserId()),
                () -> assertEquals("work", filter.getTagFilter()),
                () -> assertEquals("meeting", filter.getSearchTerm()),
                () -> assertEquals(false, filter.getDeleted()),
                () -> assertEquals(false, filter.getArchived()),
                () -> assertEquals(List.of(pinned), result.getPinnedNotes()),
                () -> assertEquals(List.of(unpinned), result.getUnpinnedNotes()),
                () -> assertEquals(List.of("work"), result.getAllTags())
        );
    }

    @Test
    void getDeletedNotes_usesSafePagingAndSortingDefaults() {
        NotePageRequest request = new NotePageRequest();
        request.setPageNo(-3);
        request.setPageSize(1000);
        request.setSortField("unknown");
        request.setSortDir("unknown");
        when(noteService.getAllNotes(any(), any())).thenReturn(new PageImpl<>(List.of()));

        NotePageData result = notePageService.getDeletedNotes(5L, request);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(noteService).getAllNotes(any(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertAll(
                () -> assertEquals(0, pageable.getPageNumber()),
                () -> assertEquals(50, pageable.getPageSize()),
                () -> assertEquals("created", result.getSortField()),
                () -> assertEquals("desc", result.getSortDir())
        );
        verify(tagService, never()).getAllTagsByUserId(any(Long.class));
    }
}
