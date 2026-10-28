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
import org.springframework.data.domain.Sort;

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
                () -> assertEquals(List.of(pinned), result.pinnedNotes()),
                () -> assertEquals(List.of(unpinned), result.unpinnedNotes()),
                () -> assertEquals(List.of("work"), result.allTags())
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
                () -> assertEquals("created", result.sortField()),
                () -> assertEquals("desc", result.sortDir()),
                () -> assertEquals(
                        Sort.by(Sort.Direction.DESC, "pinned")
                                .and(Sort.by(Sort.Direction.DESC, "created"))
                                .and(Sort.by(Sort.Direction.DESC, "id")),
                        pageable.getSort()
                )
        );
        verify(tagService, never()).getAllTagsByUserId(any(Long.class));
    }

    @Test
    void getArchivedNotes_withCustomSortAndAscending_ordersCorrectlyWithSecondaryId() {
        NotePageRequest request = new NotePageRequest();
        request.setPageNo(2);
        request.setPageSize(25);
        request.setSortField("title");
        request.setSortDir("asc");
        when(noteService.getAllNotes(any(), any())).thenReturn(new PageImpl<>(List.of()));

        NotePageData result = notePageService.getArchivedNotes(5L, request);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(noteService).getAllNotes(any(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertAll(
                () -> assertEquals(2, pageable.getPageNumber()),
                () -> assertEquals(25, pageable.getPageSize()),
                () -> assertEquals("title", result.sortField()),
                () -> assertEquals("asc", result.sortDir()),
                () -> assertEquals(
                        Sort.by(Sort.Direction.DESC, "pinned")
                                .and(Sort.by(Sort.Direction.ASC, "title"))
                                .and(Sort.by(Sort.Direction.ASC, "id")),
                        pageable.getSort()
                )
        );
    }
}
