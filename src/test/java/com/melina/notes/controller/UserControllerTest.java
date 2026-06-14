package com.melina.notes.controller;

import com.melina.notes.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(UserController.class)
@WithMockUser
class UserControllerTest {

    @MockBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signupSubmit_withValidInput_createsUserAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("name", "Melina")
                        .param("email", "user@mail.com")
                        .param("password", "password")
                        .param("confirmPassword", "password"))
                .andExpectAll(
                        status().is3xxRedirection(),
                        redirectedUrl("/login?registered=true")
                );

        verify(userService).createUser("Melina", "user@mail.com", "password");
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

        verify(userService, never()).createUser(anyString(), anyString(), anyString());
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

        verify(userService, never()).createUser(anyString(), anyString(), anyString());
    }

    @Test
    void signupSubmit_withExistingEmail_addsEmailError() throws Exception {
        when(userService.emailExists("user@mail.com")).thenReturn(true);

        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("name", "Melina")
                        .param("email", "user@mail.com")
                        .param("password", "password")
                        .param("confirmPassword", "password"))
                .andExpectAll(
                        status().isOk(),
                        view().name("signup"),
                        model().attributeHasFieldErrors("signupDto", "email")
                );

        verify(userService, never()).createUser(anyString(), anyString(), anyString());
    }
}
