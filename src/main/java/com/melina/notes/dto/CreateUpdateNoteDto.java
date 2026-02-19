package com.melina.notes.dto;

import com.melina.notes.entity.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class CreateUpdateNoteDto {
    @NotNull
    @Size(max = 30)
    private String title;

    @NotNull
    @Size(max = 10000)
    private String content;

    private Set<Tag> tags;
}
