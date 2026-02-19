package com.melina.notes.dto;

import com.melina.notes.entity.Tag;
import lombok.Data;

import java.sql.Time;
import java.util.List;

@Data
public class NoteDto {
    private Long id;
    private String title;
    private String content;
    private Time createdTime;
    private List<Tag> tags;
}
