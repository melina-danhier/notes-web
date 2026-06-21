package com.melina.notes.controller;

import com.melina.notes.dto.SignupDto;
import com.melina.notes.dto.NameUpdateDto;
import com.melina.notes.dto.PasswordUpdateDto;
import com.melina.notes.dto.DeleteAccountDto;
import com.melina.notes.entity.User;
import com.melina.notes.exception.EmailAlreadyExistsException;
import com.melina.notes.exception.RegistrationException;
import com.melina.notes.service.UserService;
import com.melina.notes.security.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Objects;

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

        try {
            userService.createUser(signupDto.getName(), signupDto.getEmail(), signupDto.getPassword());
            return "redirect:/login?registered=true";
        } catch (EmailAlreadyExistsException e) {
            bindingResult.rejectValue("email", "email.exists", "Diese E-Mail-Adresse ist bereits registriert");
        } catch (RegistrationException e) {
            bindingResult.reject("registration.failed",
                    "Registrierung fehlgeschlagen. Bitte versuchen Sie es erneut");
        }

        return "signup";
    }

    @GetMapping("/settings")
    public String settings(Model model, @AuthenticationPrincipal CustomUserDetails principal) {
        User user = userService.getUser(principal.getId());
        NameUpdateDto nameForm = new NameUpdateDto();
        nameForm.setName(user.getDisplayName());
        model.addAttribute("nameForm", nameForm);
        model.addAttribute("passwordForm", new PasswordUpdateDto());
        model.addAttribute("deleteForm", new DeleteAccountDto());
        model.addAttribute("email", user.getEmail());
        return "settings";
    }

    @PostMapping("/settings/name")
    public String updateName(
            @Valid @ModelAttribute("nameForm") NameUpdateDto form,
            BindingResult bindingResult,
            Model model,
            @AuthenticationPrincipal CustomUserDetails principal,
            Authentication authentication) {
        if (bindingResult.hasErrors()) {
            prepareSettingsModel(model, principal.getId());
            return "settings";
        }

        User user = userService.updateDisplayName(principal.getId(), form.getName());
        CustomUserDetails updatedPrincipal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken updatedAuthentication =
                new UsernamePasswordAuthenticationToken(updatedPrincipal, authentication.getCredentials(),
                        updatedPrincipal.getAuthorities());
        updatedAuthentication.setDetails(authentication.getDetails());
        SecurityContextHolder.getContext().setAuthentication(updatedAuthentication);
        return "redirect:/settings?nameUpdated=true";
    }

    @PostMapping("/settings/password")
    public String updatePassword(
            @Valid @ModelAttribute("passwordForm") PasswordUpdateDto form,
            BindingResult bindingResult,
            Model model,
            @AuthenticationPrincipal CustomUserDetails principal) {
        if (form.getNewPassword() != null && form.getConfirmPassword() != null
                && !Objects.equals(form.getNewPassword(), form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Die Passwörter stimmen nicht überein");
        }
        if (!bindingResult.hasErrors()
                && !userService.updatePassword(principal.getId(), form.getCurrentPassword(), form.getNewPassword())) {
            bindingResult.rejectValue("currentPassword", "password.invalid", "Das aktuelle Passwort ist falsch");
        }
        if (bindingResult.hasErrors()) {
            prepareSettingsModel(model, principal.getId());
            return "settings";
        }
        return "redirect:/settings?passwordUpdated=true";
    }

    @PostMapping("/settings/delete")
    public String deleteAccount(
            @Valid @ModelAttribute("deleteForm") DeleteAccountDto form,
            BindingResult bindingResult,
            Model model,
            @AuthenticationPrincipal CustomUserDetails principal,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (!bindingResult.hasErrors() && !userService.deleteAccount(principal.getId(), form.getPassword())) {
            bindingResult.rejectValue("password", "password.invalid", "Das Passwort ist falsch");
        }
        if (bindingResult.hasErrors()) {
            prepareSettingsModel(model, principal.getId());
            return "settings";
        }

        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/login?accountDeleted=true";
    }

    private void prepareSettingsModel(Model model, Long userId) {
        User user = userService.getUser(userId);
        if (!model.containsAttribute("nameForm")) {
            NameUpdateDto nameForm = new NameUpdateDto();
            nameForm.setName(user.getDisplayName());
            model.addAttribute("nameForm", nameForm);
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordUpdateDto());
        }
        if (!model.containsAttribute("deleteForm")) {
            model.addAttribute("deleteForm", new DeleteAccountDto());
        }
        model.addAttribute("email", user.getEmail());
    }
}
