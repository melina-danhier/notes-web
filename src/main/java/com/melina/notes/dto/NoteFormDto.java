package com.melina.notes.dto;

import jakarta.validation.constraints.NotBlank;
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
public class NoteFormDto {
    @Size(max = 30, message = "Der Titel darf höchstens 30 Zeichen lang sein")
    private String title;

    @NotNull(message = "Der Inhalt darf nicht fehlen")
    @Size(max = 10000, message = "Der Inhalt darf höchstens 10000 Zeichen lang sein")
    private String content;

    @NotNull(message = "Die Tag-Liste darf nicht fehlen")
    @Size(max = 10, message = "Eine Notiz darf höchstens 10 Tags enthalten")
    private List<
            @NotNull(message = "Ein Tag darf nicht fehlen")
            @NotBlank(message = "Ein Tag darf nicht leer sein")
            @Size(max = 20, message = "Ein Tag darf höchstens 20 Zeichen lang sein")
                    String
            > tags;
}
