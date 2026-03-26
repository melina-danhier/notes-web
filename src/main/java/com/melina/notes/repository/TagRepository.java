package com.melina.notes.repository;

import com.melina.notes.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag,Long> {
    Optional<Tag> findByTag(String tag);

    @Query("SELECT COUNT(n) FROM Note n JOIN n.tags t WHERE t.id = :tagId")
    long countNotesByTagId(@Param("tagId") Long tagId);
}
