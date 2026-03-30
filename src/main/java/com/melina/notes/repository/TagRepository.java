package com.melina.notes.repository;

import com.melina.notes.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByTag(String tag);

    @Query("SELECT DISTINCT t FROM Tag t JOIN t.notes n WHERE n.user.id = :userId")
    List<Tag> findAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Tag t WHERE SIZE(t.notes) = 0")
    void deleteAllWithNoNotes();
}
