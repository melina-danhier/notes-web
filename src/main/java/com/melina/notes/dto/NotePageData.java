package com.melina.notes.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
public class NotePageData {
    private Page<NoteDto> notes;
    private List<NoteDto> pinnedNotes;
    private List<NoteDto> unpinnedNotes;
    private List<String> allTags;
    private int currentPage;
    private int pageSize;
    private String sortField;
    private String sortDir;
    private String tagFilter;
    private String search;
}
