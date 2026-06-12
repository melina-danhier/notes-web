package com.melina.notes.repository;

import com.melina.notes.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByTag(String tag);

    @Query("""
        select distinct t
        from Tag t
        join t.notes n
        where n.user.id = :userId
    """)
    List<Tag> findAllByUserId(Long userId);

    @Modifying
    @Query("""
        delete from Tag t
        where t.notes is empty
    """)
    void deleteAllWithNoNotes();
}