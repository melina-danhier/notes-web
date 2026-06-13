package com.melina.notes.repository;

import com.melina.notes.entity.Note;
import com.melina.notes.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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