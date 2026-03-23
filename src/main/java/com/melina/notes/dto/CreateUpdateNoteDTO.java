package com.melina.notes.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateUpdateNoteDTO{
    private String title;
    private String content;
    private List<String> tags;
    private String tagsRaw;
}
