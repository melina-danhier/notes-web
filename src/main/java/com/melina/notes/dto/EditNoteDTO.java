package com.melina.notes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class EditNoteDTO {
    private Long id;

    @NotBlank(message = "Titel ist erforderlich")
    @Size(max = 200, message = "Titel zu lang (max 200 Zeichen)")
    private String title;

    private String content;
    private List<TagDTO> tags;
    private String tagsRaw;
}
