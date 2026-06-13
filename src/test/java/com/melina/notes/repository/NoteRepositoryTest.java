package com.melina.notes.repository;

import com.melina.notes.entity.Note;
import com.melina.notes.entity.Tag;
import com.melina.notes.entity.User;
import com.melina.notes.filter.NoteFilter;
import com.melina.notes.filter.NoteSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class NoteRepositoryTest {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Test
    void findAllByUserIdAndDeleted_deletedTrue_returnsOnlyDeletedNotesOfGivenUser() {
        User user = createAndSaveUser("user@mail.com");
        User otherUser = createAndSaveUser("other@mail.com");

        createAndSaveNote(user, "Aktive Notiz", false);
        Note deletedNote = createAndSaveNote(user, "Gelöschte Notiz", true);
        createAndSaveNote(otherUser, "Andere gelöschte Notiz", true);

        List<Note> result = noteRepository.findAllByUserIdAndDeleted(user.getId(), true);

        assertEquals(1, result.size());
        assertEquals(deletedNote.getId(), result.getFirst().getId());
    }

    @Test
    void findAllByUserIdAndDeleted_deletedFalse_returnsOnlyActiveNotesOfGivenUser() {
        User user = createAndSaveUser("user@mail.com");
        User otherUser = createAndSaveUser("other@mail.com");

        Note activeNote = createAndSaveNote(user, "Aktive Notiz", false);
        createAndSaveNote(user, "Gelöschte Notiz", true);
        createAndSaveNote(otherUser, "Andere aktive Notiz", false);

        List<Note> result = noteRepository.findAllByUserIdAndDeleted(user.getId(), false);

        assertEquals(1, result.size());
        assertEquals(activeNote.getId(), result.getFirst().getId());
    }

    @Test
    void specification_withAllFilters_returnsOnlyMatchingNote() {
        User user = createAndSaveUser("user@mail.com");
        User otherUser = createAndSaveUser("other@mail.com");
        Tag work = tagRepository.saveAndFlush(Tag.builder().tag("work").build());

        Note matching = createAndSaveNote(user, "Projekt Alpha", false);
        matching.getTags().add(work);
        noteRepository.saveAndFlush(matching);

        createAndSaveNote(user, "Projekt ohne Tag", false);
        Note deleted = createAndSaveNote(user, "Projekt Alpha", true);
        deleted.getTags().add(work);
        noteRepository.saveAndFlush(deleted);
        Note otherUsersNote = createAndSaveNote(otherUser, "Projekt Alpha", false);
        otherUsersNote.getTags().add(work);
        noteRepository.saveAndFlush(otherUsersNote);

        NoteFilter filter = NoteFilter.builder()
                .userId(user.getId())
                .searchTerm("alpha")
                .tagFilter("work")
                .deleted(false)
                .archived(false)
                .build();

        List<Note> result = noteRepository.findAll(
                NoteSpecification.build(filter), PageRequest.of(0, 10)).getContent();

        assertEquals(List.of(matching.getId()), result.stream().map(Note::getId).toList());
    }

    private User createAndSaveUser(String email) {
        User user = User.builder()
                .username("testuser")
                .email(email)
                .password("encryptedPassword")
                .role("ROLE_USER")
                .build();

        return userRepository.saveAndFlush(user);
    }

    private Note createAndSaveNote(User user, String title, boolean deleted) {
        Note note = Note.builder()
                .title(title)
                .content("Inhalt...")
                .created(Instant.now())
                .updated(Instant.now())
                .tags(new ArrayList<>())
                .user(user)
                .deleted(deleted)
                .archived(false)
                .pinned(false)
                .build();

        return noteRepository.saveAndFlush(note);
    }
}
