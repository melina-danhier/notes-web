package com.melina.notes.repository;

import com.melina.notes.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByEmail_withUsedEmail_returnRightUser() {
        User user = createAndSaveUser("user@mail.com");
        entityManager.clear();

        Optional<User> result = userRepository.findByEmail(user.getEmail());

        assertTrue(result.isPresent());
        assertAll(
                () -> assertEquals(user.getEmail(), result.get().getEmail()),
                () -> assertEquals(user.getId(), result.get().getId())
        );
    }

    @Test
    void findByEmail_withUnusedEmail_returnEmptyOptional() {
        Optional<User> result = userRepository.findByEmail("unused@mail.com");
        assertTrue(result.isEmpty());
    }

    @Test
    void existsByEmail_usedEmail_returnTrue() {
        User user = createAndSaveUser("user@mail.com");
        entityManager.clear();

        boolean result = userRepository.existsByEmail(user.getEmail());

        assertTrue(result);
    }

    @Test
    void existsByEmail_unusedEmail_returnFalse() {
        boolean result = userRepository.existsByEmail("unused@mail.com");

        assertFalse(result);
    }

    private User createAndSaveUser(String email) {
        User user = User.builder()
                .username("testuser")
                .email(email)
                .password("encryptedPassword")
                .role("ROLE_USER")
                .build();

        return userRepository.saveAndFlush(user);
    }
}
