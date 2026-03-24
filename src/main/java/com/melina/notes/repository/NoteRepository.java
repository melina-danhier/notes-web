package com.melina.notes.repository;

import com.melina.notes.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    @Query("SELECT DISTINCT n FROM Note n LEFT JOIN FETCH n.tags t WHERE n.user.id = :userId")
    List<Note> findAllByUser_Id(@Param("userId") Long userId);

    @Query("SELECT DISTINCT n FROM Note n LEFT JOIN FETCH n.tags t WHERE n.id = :id")
    Optional<Note> findById(@Param("id") Long id);
}
