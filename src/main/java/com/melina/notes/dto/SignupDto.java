package com.melina.notes.dto;

import com.melina.notes.validation.PasswordsMatch;
import com.melina.notes.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PasswordsMatch
public class SignupDto {
    @NotBlank(message = "Bitte geben Sie Ihren Namen ein")
    @Size(max = 100, message = "Der Name darf höchstens 100 Zeichen lang sein")
    private String name;

    @NotBlank(message = "Bitte geben Sie Ihre E-Mail-Adresse ein")
    @Email(message = "Bitte geben Sie eine gültige E-Mail-Adresse ein")
    @Size(max = 255, message = "Die E-Mail-Adresse darf höchstens 255 Zeichen lang sein")
    private String email;

    @NotBlank(message = "Bitte geben Sie ein Passwort ein")
    @ValidPassword
    private String password;

    @NotBlank(message = "Bitte bestätigen Sie Ihr Passwort")
    private String confirmPassword;

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
