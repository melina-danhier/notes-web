package com.melina.notes.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotePageRequest {
    private int pageNo = 0;
    private int pageSize = 9;
    private String sortField = "created";
    private String sortDir = "desc";
    private String tagFilter;
    private String search;
}
