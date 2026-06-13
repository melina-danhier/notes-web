package com.melina.notes.repository;

import com.melina.notes.entity.Note;
import com.melina.notes.entity.Tag;
import com.melina.notes.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByTag_withSavedTag_returnsOptionalWithTag() {
        User user = createAndSaveUser("user@mail.com");
        Note note = createAndSaveNote(user);
        Tag tag = createAndSaveTag(note, "work");

        Optional<Tag> foundTag = tagRepository.findByTag(tag.getTag());

        assertTrue(foundTag.isPresent());
        assertEquals(tag.getTag(), foundTag.get().getTag());
    }

    @Test
    void findByTag_withUnsavedTag_returnsEmptyOptional() {
        Optional<Tag> foundTag = tagRepository.findByTag("unsavedTag");

        assertTrue(foundTag.isEmpty());
    }

    @Test
    void findAllByUserId_returnsOnlyTagsAttachedToActiveNotesOfGivenUser() {
        User user = createAndSaveUser("user@mail.com");
        User user2 = createAndSaveUser("user2@mail.com");

        Note note = createAndSaveNote(user);
        createAndSaveTag(note, "work");
        createAndSaveTag(note, "home");
        tagRepository.saveAndFlush(Tag.builder().tag("unused").build());

        Note deletedNote = createAndSaveNote(user);
        deletedNote.setDeleted(true);
        noteRepository.saveAndFlush(deletedNote);
        createAndSaveTag(deletedNote, "deleted-only");

        List<Tag> tags = tagRepository.findAllByUserId(user.getId());
        List<Tag> tags2 = tagRepository.findAllByUserId(user2.getId());

        assertEquals(2, tags.size());
        assertTrue(tags.stream().anyMatch(tag -> tag.getTag().equals("work")));
        assertTrue(tags.stream().anyMatch(tag -> tag.getTag().equals("home")));
        assertTrue(tags.stream().noneMatch(tag -> tag.getTag().equals("unused")));
        assertTrue(tags.stream().noneMatch(tag -> tag.getTag().equals("deleted-only")));
        assertTrue(tags2.isEmpty());
    }

    @Test
    void deleteAllWithNoNotes_deletesOnlyTagsWithoutNotes() {
        User user = createAndSaveUser("user@mail.com");
        Note note = createAndSaveNote(user);

        Tag usedTag = createAndSaveTag(note, "work");

        Tag unusedTag = tagRepository.saveAndFlush(Tag.builder()
                .tag("unused")
                .notes(new HashSet<>())
                .build());

        entityManager.flush();
        entityManager.clear();

        tagRepository.deleteAllWithNoNotes();

        entityManager.flush();
        entityManager.clear();

        Optional<Tag> foundUsedTag = tagRepository.findById(usedTag.getId());
        Optional<Tag> foundUnusedTag = tagRepository.findById(unusedTag.getId());

        assertTrue(foundUsedTag.isPresent());
        assertTrue(foundUnusedTag.isEmpty());
    }

    private User createAndSaveUser(String email) {
        User user = User.builder()
                .username("Username")
                .email(email)
                .password("encryptedPass")
                .role("ROLE_USER")
                .build();

        return userRepository.saveAndFlush(user);
    }

    private Note createAndSaveNote(User user) {
        Note note = Note.builder()
                .title("Notiz")
                .content("Inhalt")
                .created(Instant.now())
                .updated(Instant.now())
                .user(user)
                .deleted(false)
                .pinned(false)
                .archived(false)
                .tags(new ArrayList<>())
                .build();

        return noteRepository.saveAndFlush(note);
    }

    private Tag createAndSaveTag(Note note, String name) {
        Tag tag = tagRepository.saveAndFlush(Tag.builder()
                .tag(name)
                .notes(new HashSet<>())
                .build());

        note.getTags().add(tag);
        tag.getNotes().add(note);

        noteRepository.saveAndFlush(note);

        return tag;
    }
}
