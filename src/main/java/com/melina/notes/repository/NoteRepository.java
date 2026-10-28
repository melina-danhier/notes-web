package com.melina.notes.repository;

import com.melina.notes.entity.Note;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long>, JpaSpecificationExecutor<Note> {

    List<Note> findAllByUserId(Long userId);

    List<Note> findAllByUserIdAndDeleted(Long userId, boolean deleted);

    @EntityGraph(attributePaths = {"tags"})
    Optional<Note> findByIdAndUserId(Long id, Long userId);
}
