package com.melina.notes.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNoteDto {
    private Long id;

    @Size(max = 200, message = "Titel zu lang (max 200 Zeichen)")
    private String title;

    private String content;

    private List<@Size(max = 50, message = "Tag zu lang (max 50 Zeichen)") String> tags;
}
