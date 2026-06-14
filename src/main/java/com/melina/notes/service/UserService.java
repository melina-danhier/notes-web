package com.melina.notes.service;

import com.melina.notes.entity.User;
import com.melina.notes.exception.EmailAlreadyExistsException;
import com.melina.notes.exception.RegistrationException;
import com.melina.notes.exception.UserNotFoundException;
import com.melina.notes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
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
                    .username(normalizeName(name))
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(password))
                    .role("ROLE_USER")
                    .build();

            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        } catch (DataAccessException ex) {
            throw new RegistrationException(ex);
        } catch (RuntimeException ex) {
            throw new RegistrationException(ex);
        }
    }

    @Transactional(readOnly = true)
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
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
