package com.melina.notes.filter;

import com.melina.notes.entity.Note;
import com.melina.notes.entity.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class NoteSpecification {
    public static Specification<Note> build(NoteFilter filter) {
        return (root, query, cb) -> {
            Predicate predicate = Specification
                    .where(isUser(filter.getUserId()))
                    .and(searchInTitleOrContent(filter.getSearchTerm()))
                    .and(hasTag(filter.getTagFilter()))
                    .and(isDeleted(filter.getDeleted()))
                    .toPredicate(root, query, cb);
            assert query != null;
            query.distinct(true);
            return predicate;
        };
    }

    private static Specification<Note> isUser(Long userId) {
        return (root, query, cb) -> userId == null ? null :
                cb.equal(root.get("user").get("id"), userId);
    }

    private static Specification<Note> searchInTitleOrContent(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return null;
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("content")), pattern)
            );
        };
    }

    private static Specification<Note> hasTag(String tag) {
        return (root, query, cb) -> {
            if (tag == null || tag.isBlank()) return null;
            Join<Note, Tag> tags = root.join("tags", JoinType.LEFT);
            return cb.equal(tags.get("tag"), tag);
        };
    }

    private static Specification<Note> isDeleted(Boolean deleted) {
        return (root, query, cb) -> {
            if (deleted == null) {
                return cb.isFalse(root.get("deleted"));
            }
            return cb.equal(root.get("deleted"), deleted);
        };
    }

}
