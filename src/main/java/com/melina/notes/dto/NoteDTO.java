package com.melina.notes.dto;

import lombok.Data;

import java.sql.Time;
import java.time.Instant;
import java.util.List;

@Data
public class NoteDTO {
    private String title;
    private String content;
    private Instant created;
    private Instant updated;
    private List<String> tags;
    private Long userId;
}
