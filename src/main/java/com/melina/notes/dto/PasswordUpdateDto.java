package com.melina.notes.dto;

import com.melina.notes.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordUpdateDto {
    @NotBlank(message = "Bitte geben Sie Ihr aktuelles Passwort ein")
    @Size(max = 72, message = "Das Passwort darf höchstens 72 Zeichen lang sein")
    private String currentPassword;

    @NotBlank(message = "Bitte geben Sie ein neues Passwort ein")
    @ValidPassword
    private String newPassword;

    @NotBlank(message = "Bitte bestätigen Sie das neue Passwort")
    private String confirmPassword;
}
