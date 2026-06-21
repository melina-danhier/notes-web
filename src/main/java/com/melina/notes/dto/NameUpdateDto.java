package com.melina.notes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NameUpdateDto {
    @NotBlank(message = "Bitte geben Sie einen Namen ein")
    @Size(max = 100, message = "Der Name darf höchstens 100 Zeichen lang sein")
    private String name;

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }
}
