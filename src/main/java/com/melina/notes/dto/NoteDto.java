package com.melina.notes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteDto {
    private Long id;
    private String title;
    private String content;
    private String contentPreview;
    private Instant created;
    private Instant updated;
    private List<String> tags;
    private Long userId;
    private boolean pinned;
    private boolean archived;
}
