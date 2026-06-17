package com.melina.notes.repository;

import com.melina.notes.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long>, JpaSpecificationExecutor<Note> {

    List<Note> findAllByUserIdAndDeleted(Long userId, boolean deleted);

    Optional<Note> findByIdAndUserId(Long id, Long userId);
}