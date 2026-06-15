package com.melina.notes.dto;

import lombok.Data;

@Data
public class NotePageRequest {
    private int pageNo = 0;
    private int pageSize = 6;
    private String sortField = "created";
    private String sortDir = "desc";
    private String tagFilter;
    private String search;
}
