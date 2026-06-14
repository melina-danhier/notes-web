package com.melina.notes.controller;

import com.melina.notes.dto.SignupDto;
import com.melina.notes.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    public String signupSubmit(
            @Valid @ModelAttribute("signupDto") SignupDto signupDto,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "signup";
        }

        if (userService.emailExists(signupDto.getEmail())) {
            bindingResult.rejectValue("email", "email.exists", "Diese E-Mail-Adresse ist bereits registriert");
            return "signup";
        }

        try {
            userService.createUser(signupDto.getName(), signupDto.getEmail(), signupDto.getPassword());
            return "redirect:/login?registered=true";
        } catch (Exception e) {
            bindingResult.reject("registration.failed",
                    "Registrierung fehlgeschlagen. Bitte versuchen Sie es erneut");
            return "signup";
        }
    }
}
