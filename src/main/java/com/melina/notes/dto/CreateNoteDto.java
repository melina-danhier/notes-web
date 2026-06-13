package com.melina.notes.dto;

import jakarta.validation.constraints.NotNull;
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
    @Size(max = 30, message = "Titel zu lang (max 30 Zeichen)")
    @NotNull
    private String title;

    @Size(max = 10000, message = "Inhalt zu lang (max 10000 Zeichen)")
    @NotNull
    private String content;

    @NotNull
    private List<
            @Size(max = 20, message = "Tag zu lang (max 20 Zeichen)")
            @NotNull
                    String
            > tags;
}
