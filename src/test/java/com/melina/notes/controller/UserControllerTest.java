package com.melina.notes.controller;

import com.melina.notes.exception.EmailAlreadyExistsException;
import com.melina.notes.exception.RegistrationException;
import com.melina.notes.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(UserController.class)
@WithMockUser
@ActiveProfiles("test")
class UserControllerTest {

    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signupSubmit_withValidInput_createsUserAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("name", "Melina")
                        .param("email", "user@mail.com")
                        .param("password", "validPassword")
                        .param("confirmPassword", "validPassword"))
                .andExpectAll(
                        status().is3xxRedirection(),
                        redirectedUrl("/login?registered=true")
                );

        verify(userService).createUser("Melina", "user@mail.com", "validPassword");
    }

    @Test
    void signupSubmit_withInvalidFields_returnsFormWithFieldErrors() throws Exception {
        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("name", "")
                        .param("email", "keine-email")
                        .param("password", "123")
                        .param("confirmPassword", "anders"))
                .andExpectAll(
                        status().isOk(),
                        view().name("signup"),
                        model().attributeHasFieldErrors(
                                "signupDto", "name", "email", "password", "confirmPassword")
                );

        verify(userService, never()).createUser(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void signupSubmit_withPasswordOverBcryptLimit_returnsPasswordError() throws Exception {
        String password = "x".repeat(73);

        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("name", "Melina")
                        .param("email", "user@mail.com")
                        .param("password", password)
                        .param("confirmPassword", password))
                .andExpectAll(
                        status().isOk(),
                        view().name("signup"),
                        model().attributeHasFieldErrors("signupDto", "password")
                );

        verify(userService, never()).createUser(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void signupSubmit_withExistingEmail_addsEmailError() throws Exception {
        doThrow(new EmailAlreadyExistsException("user@mail.com")).when(userService)
                .createUser("Melina", "user@mail.com", "validPassword");

        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("name", "Melina")
                        .param("email", "user@mail.com")
                        .param("password", "validPassword")
                        .param("confirmPassword", "validPassword"))
                .andExpectAll(
                        status().isOk(),
                        view().name("signup"),
                        model().attributeHasFieldErrors("signupDto", "email")
                );
    }

    @Test
    void signupSubmit_whenCreationFails_addsGlobalError() throws Exception {
        doThrow(new RegistrationException(new IllegalStateException())).when(userService)
                .createUser("Melina", "user@mail.com", "validPassword");

        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("name", "Melina")
                        .param("email", "user@mail.com")
                        .param("password", "validPassword")
                        .param("confirmPassword", "validPassword"))
                .andExpectAll(
                        status().isOk(),
                        view().name("signup"),
                        model().attributeHasErrors("signupDto")
                );
    }
}
