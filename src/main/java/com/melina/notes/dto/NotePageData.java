package com.melina.notes.dto;

import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

@Builder
public record NotePageData(
        Page<NoteDto> notes,
        List<NoteDto> pinnedNotes,
        List<NoteDto> unpinnedNotes,
        List<String> allTags,
        int currentPage,
        int pageSize,
        String sortField,
        String sortDir,
        String tagFilter,
        String search
) {
}
