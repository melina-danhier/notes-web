package com.melina.notes.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteFilter {

    private Long userId;
    private String tagFilter;
    private String searchTerm;
    private Boolean deleted;
    private Boolean archived;
}