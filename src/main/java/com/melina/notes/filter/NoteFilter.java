package com.melina.notes.filter;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NoteFilter {
    private long userId;
    private String tagFilter;
    private String searchTerm;
    private Boolean deleted;
}
