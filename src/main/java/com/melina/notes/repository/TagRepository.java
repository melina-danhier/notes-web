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
        from Note n
        join n.tags t
        where n.user.id = :userId
          and n.deleted = false
    """)
    List<Tag> findAllByUserId(Long userId);

    @Modifying
    @Query(
            value = """
                delete from tags t
                where not exists (
                    select 1
                    from note_tag nt
                    where nt.tag_id = t.id
                )
            """,
            nativeQuery = true
    )
    int deleteAllWithNoNotes();
}
