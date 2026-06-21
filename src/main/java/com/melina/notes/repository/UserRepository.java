package com.melina.notes.repository;

import com.melina.notes.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByDemoAccountTrue();

    List<User> findAllByDemoAccountTrueAndExpiresAtBefore(Instant expiresAt);
}
