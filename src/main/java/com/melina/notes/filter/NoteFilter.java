package com.melina.notes.filter;

import lombok.Data;

@Data
public class NoteFilter {
    private long userId;
    private String tagFilter;
    private String searchTerm;
    private Boolean deleted;
}
