package com.melina.notes.controller;

import com.melina.notes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    public String signupSubmit(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String password,
                               @RequestParam String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            return "redirect:/signup?error=password_mismatch";
        }
        if (userService.emailExists(email)) {
            return "redirect:/signup?error=email_exists";
        }
        userService.createUser(name,email,password);
        return "redirect:/notes?registered=true";
    }
}
