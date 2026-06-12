package com.melina.notes.service;

import com.melina.notes.entity.User;
import com.melina.notes.exception.UserNotFoundException;
import com.melina.notes.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void emailExists_existingEmail_returnsTrue() {
        when(userRepository.existsByEmail("test@mail.com")).thenReturn(true);

        boolean result = userService.emailExists("test@mail.com");

        assertTrue(result);
        verify(userRepository).existsByEmail("test@mail.com");
    }

    @Test
    void emailExists_unknownEmail_returnsFalse() {
        when(userRepository.existsByEmail("test2@mail.com")).thenReturn(false);

        boolean result = userService.emailExists("test2@mail.com");

        assertFalse(result);
        verify(userRepository).existsByEmail("test2@mail.com");
    }

    @Test
    void emailExists_normalizesEmailBeforeSearching() {
        when(userRepository.existsByEmail("test@mail.com")).thenReturn(true);

        boolean result = userService.emailExists("  Test@Mail.com  ");

        assertTrue(result);
        verify(userRepository).existsByEmail("test@mail.com");
    }

    @Test
    void createUser_validInput_encodesPasswordAndSavesUser() {
        when(userRepository.existsByEmail("melina@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("encodedPassword");

        userService.createUser("Melina", "melina@mail.com", "1234");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertAll(
                () -> assertEquals("Melina", savedUser.getUsername()),
                () -> assertEquals("melina@mail.com", savedUser.getEmail()),
                () -> assertEquals("encodedPassword", savedUser.getPassword()),
                () -> assertEquals("ROLE_USER", savedUser.getRole())
        );

        verify(userRepository).existsByEmail("melina@mail.com");
        verify(passwordEncoder).encode("1234");
    }

    @Test
    void createUser_trimsNameAndNormalizesEmailBeforeSaving() {
        when(userRepository.existsByEmail("melina@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("encodedPassword");

        userService.createUser("  Melina  ", "  Melina@Mail.com  ", "1234");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertAll(
                () -> assertEquals("Melina", savedUser.getUsername()),
                () -> assertEquals("melina@mail.com", savedUser.getEmail()),
                () -> assertEquals("encodedPassword", savedUser.getPassword()),
                () -> assertEquals("ROLE_USER", savedUser.getRole())
        );

        verify(userRepository).existsByEmail("melina@mail.com");
        verify(passwordEncoder).encode("1234");
    }

    @Test
    void createUser_existingEmail_throwsExceptionAndDoesNotSaveUser() {
        when(userRepository.existsByEmail("melina@mail.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("Melina", "melina@mail.com", "1234"));

        verify(userRepository).existsByEmail("melina@mail.com");
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUser_existingId_returnsUser() {
        User user = createUser(2L, "Test", "test@mail.com", "encodedPassword");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        User result = userService.getUser(2L);

        assertSame(user, result);
        verify(userRepository).findById(2L);
    }

    @Test
    void getUser_unknownId_throwsUserNotFoundException() {
        when(userRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.getUser(3L));

        verify(userRepository).findById(3L);
    }

    private User createUser(Long id, String username, String email, String password) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole("ROLE_USER");
        return user;
    }
}