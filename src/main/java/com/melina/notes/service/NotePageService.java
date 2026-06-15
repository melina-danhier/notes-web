package com.melina.notes.service;

import com.melina.notes.dto.NoteDto;
import com.melina.notes.dto.NotePageData;
import com.melina.notes.dto.NotePageRequest;
import com.melina.notes.filter.NoteFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotePageService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("created", "updated", "title");
    private static final int MAX_PAGE_SIZE = 50;

    private final NoteService noteService;
    private final TagService tagService;

    public NotePageData getActiveNotes(Long userId, NotePageRequest request) {
        NoteFilter filter = NoteFilter.builder()
                .userId(userId)
                .tagFilter(request.getTagFilter())
                .searchTerm(request.getSearch())
                .deleted(false)
                .archived(false)
                .build();

        return loadPage(userId, request, filter, true);
    }

    public NotePageData getArchivedNotes(Long userId, NotePageRequest request) {
        NoteFilter filter = NoteFilter.builder()
                .userId(userId)
                .deleted(false)
                .archived(true)
                .build();

        return loadPage(userId, request, filter, false);
    }

    public NotePageData getDeletedNotes(Long userId, NotePageRequest request) {
        NoteFilter filter = NoteFilter.builder()
                .userId(userId)
                .deleted(true)
                .build();

        return loadPage(userId, request, filter, false);
    }

    private NotePageData loadPage(Long userId, NotePageRequest request, NoteFilter filter, boolean loadTags) {
        int pageNo = Math.max(request.getPageNo(), 0);
        int pageSize = Math.clamp(request.getPageSize(), 1, MAX_PAGE_SIZE);
        String sortField = resolveSortField(request.getSortField());
        Sort.Direction sortDirection = resolveSortDirection(request.getSortDir());

        Sort sort = Sort.by(Sort.Direction.DESC, "pinned")
                .and(Sort.by(sortDirection, sortField));
        Page<NoteDto> notes = noteService.getAllNotes(filter, PageRequest.of(pageNo, pageSize, sort));

        List<NoteDto> pinnedNotes = notes.getContent().stream()
                .filter(NoteDto::isPinned)
                .toList();
        List<NoteDto> unpinnedNotes = notes.getContent().stream()
                .filter(note -> !note.isPinned())
                .toList();
        List<String> tags = loadTags ? tagService.getAllTagsByUserId(userId) : List.of();

        return NotePageData.builder()
                .notes(notes)
                .pinnedNotes(pinnedNotes)
                .unpinnedNotes(unpinnedNotes)
                .allTags(tags)
                .currentPage(pageNo)
                .pageSize(pageSize)
                .sortField(sortField)
                .sortDir(sortDirection.name().toLowerCase())
                .tagFilter(request.getTagFilter())
                .search(request.getSearch())
                .build();
    }

    private String resolveSortField(String sortField) {
        if (ALLOWED_SORT_FIELDS.contains(sortField)) {
            return sortField;
        }
        return "created";
    }

    private Sort.Direction resolveSortDirection(String sortDir) {
        if ("asc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.ASC;
        }
        return Sort.Direction.DESC;
    }
}
