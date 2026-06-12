package com.melina.notes.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class NoteDto {
    private Long id;
    private String title;
    private String content;
    private String contentPreview;
    private Instant created;
    private Instant updated;
    private List<TagDto> tags;
    private Long userId;
    private boolean pinned;
    private boolean archived;
}
