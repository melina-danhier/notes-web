package com.melina.notes.controller;

import com.melina.notes.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void signupSubmit_withValidInput_createsUserAndRedirectsToLogin() {
        String result = userController.signupSubmit(
                "Melina", "user@mail.com", "password", "password");

        assertEquals("redirect:/login?registered=true", result);
        verify(userService).createUser("Melina", "user@mail.com", "password");
    }

    @Test
    void signupSubmit_withShortPassword_rejectsInputWithoutServiceCall() {
        String result = userController.signupSubmit(
                "Melina", "user@mail.com", "123", "123");

        assertEquals("redirect:/signup?error=password_too_short", result);
        verifyNoInteractions(userService);
    }

    @Test
    void signupSubmit_withExistingEmail_doesNotCreateUser() {
        when(userService.emailExists("user@mail.com")).thenReturn(true);

        String result = userController.signupSubmit(
                "Melina", "user@mail.com", "password", "password");

        assertEquals("redirect:/signup?error=email_exists", result);
        verify(userService, never()).createUser(anyString(), anyString(), anyString());
    }

    @Test
    void signupSubmit_whenCreationFails_redirectsWithError() {
        doThrow(new IllegalStateException()).when(userService)
                .createUser("Melina", "user@mail.com", "password");

        String result = userController.signupSubmit(
                "Melina", "user@mail.com", "password", "password");

        assertEquals("redirect:/signup?error=registration_failed", result);
    }
}
