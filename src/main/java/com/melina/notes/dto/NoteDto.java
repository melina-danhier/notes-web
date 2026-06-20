package com.melina.notes.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Builder
@Getter
@Setter
public class NoteDto {
    private Long id;
    private String title;
    private String content;
    private String contentPreview;
    private Instant created;
    private Instant updated;
    private List<String> tags;
    private boolean pinned;
    private boolean archived;
}
