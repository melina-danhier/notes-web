package com.melina.notes.repository;

import com.melina.notes.entity.Note;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// NoteRepository.java
@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.tags t WHERE n.user.id = :userId")
    List<Note> findAllByUser_Id(@Param("userId") Long userId);

    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.tags t WHERE n.id = :id")
    @NullMarked Optional<Note> findById(@Param("id") Long id);
}
