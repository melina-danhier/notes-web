package com.melina.notes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteAccountDto {
    @NotBlank(message = "Bitte geben Sie Ihr Passwort ein")
    @Size(max = 72, message = "Das Passwort darf höchstens 72 Zeichen lang sein")
    private String password;
}
