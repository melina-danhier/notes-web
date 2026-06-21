package com.melina.notes.service;

import com.melina.notes.entity.Note;
import com.melina.notes.entity.User;
import com.melina.notes.exception.EmailAlreadyExistsException;
import com.melina.notes.exception.RegistrationException;
import com.melina.notes.exception.UserNotFoundException;
import com.melina.notes.repository.NoteRepository;
import com.melina.notes.repository.TagRepository;
import com.melina.notes.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("SameParameterValue")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private TagRepository tagRepository;

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
                () -> assertEquals("Melina", savedUser.getDisplayName()),
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
                () -> assertEquals("Melina", savedUser.getDisplayName()),
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

        assertThrows(EmailAlreadyExistsException.class,
                () -> userService.createUser("Melina", "melina@mail.com", "1234"));

        verify(userRepository).existsByEmail("melina@mail.com");
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_passwordEncodingFails_throwsRegistrationException() {
        when(userRepository.existsByEmail("melina@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenThrow(new IllegalStateException("Encoder unavailable"));

        assertThrows(RegistrationException.class,
                () -> userService.createUser("Melina", "melina@mail.com", "1234"));

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

    @Test
    void updatePassword_withCorrectCurrentPassword_encodesAndStoresNewPassword() {
        User user = createUser(4L, "Melina", "test@mail.com", "oldEncoded");
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "oldEncoded")).thenReturn(true);
        when(passwordEncoder.encode("newPassword1")).thenReturn("newEncoded");

        assertTrue(userService.updatePassword(4L, "oldPassword", "newPassword1"));
        assertEquals("newEncoded", user.getPassword());
    }

    @Test
    void updatePassword_withWrongCurrentPassword_keepsPassword() {
        User user = createUser(4L, "Melina", "test@mail.com", "oldEncoded");
        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "oldEncoded")).thenReturn(false);

        assertFalse(userService.updatePassword(4L, "wrong", "newPassword1"));
        assertEquals("oldEncoded", user.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void deleteAccount_withCorrectPassword_removesNotesUserAndUnusedTags() {
        User user = createUser(8L, "Melina", "test@mail.com", "encoded");
        Note note = Note.builder().tags(new java.util.LinkedList<>()).build();
        when(userRepository.findById(8L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(noteRepository.findAllByUserId(8L)).thenReturn(List.of(note));

        assertTrue(userService.deleteAccount(8L, "password"));

        verify(noteRepository).deleteAll(List.of(note));
        verify(userRepository).delete(user);
        verify(tagRepository).deleteAllWithNoNotes();
    }

    private User createUser(Long id, String displayName, String email, String password) {
        User user = new User();
        user.setId(id);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole("ROLE_USER");
        return user;
    }
}
