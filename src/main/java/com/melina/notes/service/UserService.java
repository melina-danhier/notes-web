package com.melina.notes.service;

import com.melina.notes.entity.User;
import com.melina.notes.entity.Note;
import com.melina.notes.exception.EmailAlreadyExistsException;
import com.melina.notes.exception.RegistrationException;
import com.melina.notes.exception.UserNotFoundException;
import com.melina.notes.repository.UserRepository;
import com.melina.notes.repository.NoteRepository;
import com.melina.notes.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final TagRepository tagRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(normalizeEmail(email));
    }

    public void createUser(String name, String email, String password) {
        String normalizedEmail = normalizeEmail(email);

        if (emailExists(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        try {
            User user = User.builder()
                    .displayName(normalizeName(name))
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(password))
                    .role("ROLE_USER")
                    .build();

            userRepository.save(user);
            log.info("User registered - userId={}", user.getId());
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        } catch (RuntimeException ex) {
            throw new RegistrationException(ex);
        }
    }

    @Transactional(readOnly = true)
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public User updateDisplayName(Long userId, String name) {
        User user = getUser(userId);
        user.setDisplayName(normalizeName(name));
        log.info("User display name updated - userId={}", userId);
        return user;
    }

    public boolean updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        log.info("User password updated - userId={}", userId);
        return true;
    }

    public boolean deleteAccount(Long userId, String password) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return false;
        }

        List<Note> notes = noteRepository.findAllByUserId(userId);
        notes.forEach(note -> note.getTags().clear());
        noteRepository.deleteAll(notes);
        noteRepository.flush();
        userRepository.delete(user);
        userRepository.flush();
        tagRepository.deleteAllWithNoNotes();
        log.info("User account deleted - userId={}", userId);
        return true;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }

        return email.trim().toLowerCase();
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }

        return name.trim();
    }
}
