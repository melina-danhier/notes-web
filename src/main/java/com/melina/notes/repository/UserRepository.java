package com.melina.notes.repository;

import com.melina.notes.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> getUserById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}
