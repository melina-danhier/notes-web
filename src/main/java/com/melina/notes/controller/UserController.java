package com.melina.notes.controller;

import com.melina.notes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    public String signupSubmit(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String password,
                               @RequestParam String confirmPassword) {
        // Validiere leere Felder
        if (name == null || name.trim().isEmpty()) {
            return "redirect:/signup?error=empty_name";
        }
        if (email == null || email.trim().isEmpty()) {
            return "redirect:/signup?error=empty_email";
        }
        if (password == null || password.isEmpty()) {
            return "redirect:/signup?error=empty_password";
        }

        // Validiere Passwort-Länge
        if (password.length() < 6) {
            return "redirect:/signup?error=password_too_short";
        }

        // Validiere Passwort-Bestätigung
        if (!password.equals(confirmPassword)) {
            return "redirect:/signup?error=password_mismatch";
        }

        // Validiere Email-Existenz
        if (userService.emailExists(email)) {
            return "redirect:/signup?error=email_exists";
        }

        try {
            userService.createUser(name, email, password);
            return "redirect:/login?registered=true";
        } catch (Exception e) {
            return "redirect:/signup?error=registration_failed";
        }
    }
}
