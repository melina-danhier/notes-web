package com.melina.notes.repository;

import com.melina.notes.entity.User;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> getUserById(@NonNull Long id);
    Optional<User> findByUsername(@NonNull String username);
    Optional<User> findByEmail(String email);
}
