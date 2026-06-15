package com.melina.notes.filter;

import com.melina.notes.entity.Note;
import com.melina.notes.entity.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.Objects;

public final class NoteSpecification {

    private static final char LIKE_ESCAPE_CHAR = '\\';

    private NoteSpecification() {
    }

    public static Specification<Note> build(NoteFilter filter) {
        Objects.requireNonNull(filter, "NoteFilter darf nicht null sein");
        Objects.requireNonNull(filter.getUserId(), "userId darf nicht null sein");

        return (root, query, cb) -> Specification
                .where(isUser(filter.getUserId()))
                .and(searchInTitleOrContent(filter.getSearchTerm()))
                .and(hasTag(filter.getTagFilter()))
                .and(isDeleted(filter.getDeleted()))
                .and(isArchived(filter.getArchived()))
                .toPredicate(root, query, cb);
    }

    private static Specification<Note> isUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    private static Specification<Note> searchInTitleOrContent(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return null;
            }

            String pattern = "%" + escapeLikePattern(search.trim().toLowerCase(Locale.ROOT)) + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE_CHAR),
                    cb.like(cb.lower(root.get("content")), pattern, LIKE_ESCAPE_CHAR)
            );
        };
    }

    private static Specification<Note> hasTag(String tag) {
        return (root, query, cb) -> {
            if (tag == null || tag.isBlank()) {
                return null;
            }

            Join<Note, Tag> tags = root.join("tags", JoinType.LEFT);
            if (query != null) {
                query.distinct(true);
            }
            return cb.equal(tags.get("tag"), tag.trim());
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

    private static Specification<Note> isArchived(Boolean archived) {
        return (root, query, cb) -> archived == null
                ? null
                : cb.equal(root.get("archived"), archived);
    }

    private static String escapeLikePattern(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
