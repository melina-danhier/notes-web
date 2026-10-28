package com.melina.notes.repository;

import com.melina.notes.entity.Note;
import com.melina.notes.entity.Tag;
import com.melina.notes.entity.User;
import com.melina.notes.filter.NoteFilter;
import com.melina.notes.filter.NoteSpecification;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
class NoteRepositoryTest {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TestEntityManager entityManager;

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

    @Test
    void specification_withoutUserId_throwsException() {
        NoteFilter filter = NoteFilter.builder()
                .deleted(false)
                .build();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> NoteSpecification.build(filter)
        );

        assertEquals("userId darf nicht null sein", exception.getMessage());
    }

    @Test
    void specification_searchTreatsLikeWildcardsAsLiteralCharacters() {
        User user = createAndSaveUser("user@mail.com");
        Note matching = createAndSaveNote(user, "Fortschritt 100%", false);
        createAndSaveNote(user, "Normale Notiz", false);

        NoteFilter filter = NoteFilter.builder()
                .userId(user.getId())
                .searchTerm("%")
                .deleted(false)
                .archived(false)
                .build();

        List<Note> result = noteRepository.findAll(
                NoteSpecification.build(filter),
                PageRequest.of(0, 10)
        ).getContent();

        assertEquals(List.of(matching.getId()), result.stream().map(Note::getId).toList());
    }

    @Test
    void specification_searchInContent_matchesWhenTitleDoesNotMatch() {
        User user = createAndSaveUser("contentsearch@mail.com");
        Note note = createAndSaveNote(user, "Alltägliches", "Milch und Brot einkaufen", false, false);
        createAndSaveNote(user, "Arbeit", "Bericht fertigstellen", false, false);

        NoteFilter filter = NoteFilter.builder()
                .userId(user.getId())
                .searchTerm("milch")
                .deleted(false)
                .archived(false)
                .build();

        List<Note> result = noteRepository.findAll(
                NoteSpecification.build(filter), PageRequest.of(0, 10)).getContent();

        assertEquals(List.of(note.getId()), result.stream().map(Note::getId).toList());
    }

    @Test
    void specification_withEmptyAndWhitespaceFilters_ignoresThem() {
        User user = createAndSaveUser("empty@mail.com");
        createAndSaveNote(user, "Erste Notiz", false);
        createAndSaveNote(user, "Zweite Notiz", false);

        NoteFilter filter = NoteFilter.builder()
                .userId(user.getId())
                .searchTerm("   ")
                .tagFilter("")
                .deleted(false)
                .archived(false)
                .build();

        List<Note> result = noteRepository.findAll(
                NoteSpecification.build(filter), PageRequest.of(0, 10)).getContent();

        assertEquals(2, result.size());
    }

    @Test
    void specification_withArchivedNull_returnsBothArchivedAndUnarchived() {
        User user = createAndSaveUser("archivednull@mail.com");
        Note active = createAndSaveNote(user, "Aktiv", "...", false, false);
        Note archived = createAndSaveNote(user, "Archiviert", "...", false, true);

        NoteFilter filter = NoteFilter.builder()
                .userId(user.getId())
                .archived(null)
                .deleted(false)
                .build();

        List<Note> result = noteRepository.findAll(
                NoteSpecification.build(filter), PageRequest.of(0, 10)).getContent();

        assertEquals(2, result.size());
        assertTrue(result.stream().map(Note::getId).toList().containsAll(List.of(active.getId(), archived.getId())));
    }

    @Test
    void specification_wildcardsUnderscoreAndBackslash_treatedLiterally() {
        User user = createAndSaveUser("wildcard@mail.com");
        Note matchingUnderscore = createAndSaveNote(user, "var_name", "Inhalt...", false, false);
        createAndSaveNote(user, "var-name", "Inhalt...", false, false);

        NoteFilter filterUnderscore = NoteFilter.builder()
                .userId(user.getId())
                .searchTerm("var_name")
                .deleted(false)
                .archived(false)
                .build();

        List<Note> underscoreResult = noteRepository.findAll(
                NoteSpecification.build(filterUnderscore), PageRequest.of(0, 10)).getContent();

        assertEquals(List.of(matchingUnderscore.getId()), underscoreResult.stream().map(Note::getId).toList());

        Note matchingBackslash = createAndSaveNote(user, "path\\to\\file", "Inhalt...", false, false);
        NoteFilter filterBackslash = NoteFilter.builder()
                .userId(user.getId())
                .searchTerm("\\to\\")
                .deleted(false)
                .archived(false)
                .build();

        List<Note> backslashResult = noteRepository.findAll(
                NoteSpecification.build(filterBackslash), PageRequest.of(0, 10)).getContent();

        assertEquals(List.of(matchingBackslash.getId()), backslashResult.stream().map(Note::getId).toList());
    }

    @Test
    void specification_distinctResults_whenNoteHasMultipleTags() {
        User user = createAndSaveUser("distinct@mail.com");
        Tag t1 = tagRepository.saveAndFlush(Tag.builder().tag("dev").build());
        Tag t2 = tagRepository.saveAndFlush(Tag.builder().tag("java").build());

        Note noteWithTwoTags = createAndSaveNoteWithTags(user, "Multi-Tag Note", "Inhalt", List.of(t1, t2));

        NoteFilter filter = NoteFilter.builder()
                .userId(user.getId())
                .tagFilter("dev")
                .deleted(false)
                .archived(false)
                .build();

        Page<Note> result = noteRepository.findAll(
                NoteSpecification.build(filter), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(List.of(noteWithTwoTags.getId()), result.getContent().stream().map(Note::getId).toList());
    }

    @Test
    void findByIdAndUserId_eagerlyLoadsTagsViaEntityGraph() {
        User user = createAndSaveUser("graph@mail.com");
        Tag tag1 = tagRepository.saveAndFlush(Tag.builder().tag("backend").build());
        Tag tag2 = tagRepository.saveAndFlush(Tag.builder().tag("jpa").build());

        Note note = createAndSaveNoteWithTags(user, "Graph Note", "Content", List.of(tag1, tag2));
        entityManager.flush();
        entityManager.clear();

        Optional<Note> found = noteRepository.findByIdAndUserId(note.getId(), user.getId());
        assertTrue(found.isPresent());
        assertTrue(Hibernate.isInitialized(found.get().getTags()), "Tags must be initialized eagerly by EntityGraph");
        assertEquals(2, found.get().getTags().size());
        assertEquals(Set.of("backend", "jpa"), Set.of(found.get().getTags().get(0).getTag(), found.get().getTags().get(1).getTag()));
    }

    @Test
    void batchFetching_loadsTagsForMultipleNotes() {
        User user = createAndSaveUser("batch@mail.com");
        Tag tag1 = tagRepository.saveAndFlush(Tag.builder().tag("tag1").build());
        Tag tag2 = tagRepository.saveAndFlush(Tag.builder().tag("tag2").build());
        Tag tag3 = tagRepository.saveAndFlush(Tag.builder().tag("tag3").build());

        createAndSaveNoteWithTags(user, "Note 1", "Content 1", List.of(tag1));
        createAndSaveNoteWithTags(user, "Note 2", "Content 2", List.of(tag2));
        createAndSaveNoteWithTags(user, "Note 3", "Content 3", List.of(tag3));
        entityManager.flush();
        entityManager.clear();

        List<Note> notes = noteRepository.findAllByUserId(user.getId());
        assertEquals(3, notes.size());
        for (Note n : notes) {
            assertNotNull(n.getTags());
            assertEquals(1, n.getTags().size());
        }
    }

    @Test
    void pagination_stableResultsAcrossPages_withIdenticalTimestampsAndTitles() {
        User user = createAndSaveUser("paging@mail.com");
        Instant fixedTime = Instant.parse("2026-01-01T10:00:00Z");

        List<Long> savedIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Note note = Note.builder()
                    .title("Gleicher Titel")
                    .content("Inhalt " + i)
                    .created(fixedTime)
                    .updated(fixedTime)
                    .tags(new ArrayList<>())
                    .user(user)
                    .deleted(false)
                    .archived(false)
                    .pinned(false)
                    .build();
            savedIds.add(noteRepository.saveAndFlush(note).getId());
        }

        Sort stableSort = Sort.by(Sort.Direction.DESC, "pinned")
                .and(Sort.by(Sort.Direction.DESC, "created"))
                .and(Sort.by(Sort.Direction.DESC, "id"));

        NoteFilter filter = NoteFilter.builder()
                .userId(user.getId())
                .deleted(false)
                .archived(false)
                .build();

        Page<Note> page0 = noteRepository.findAll(NoteSpecification.build(filter), PageRequest.of(0, 2, stableSort));
        Page<Note> page1 = noteRepository.findAll(NoteSpecification.build(filter), PageRequest.of(1, 2, stableSort));
        Page<Note> page2 = noteRepository.findAll(NoteSpecification.build(filter), PageRequest.of(2, 2, stableSort));

        assertEquals(5, page0.getTotalElements());
        assertEquals(2, page0.getContent().size());
        assertEquals(2, page1.getContent().size());
        assertEquals(1, page2.getContent().size());

        Set<Long> collectedIds = new HashSet<>();
        page0.getContent().forEach(n -> collectedIds.add(n.getId()));
        page1.getContent().forEach(n -> collectedIds.add(n.getId()));
        page2.getContent().forEach(n -> collectedIds.add(n.getId()));

        assertEquals(5, collectedIds.size(), "No duplicates across pages and all 5 items retrieved");
        assertTrue(collectedIds.containsAll(savedIds));
    }

    private User createAndSaveUser(String email) {
        User user = User.builder()
                .displayName("testuser")
                .email(email)
                .password("encryptedPassword")
                .role("ROLE_USER")
                .build();

        return userRepository.saveAndFlush(user);
    }

    private Note createAndSaveNote(User user, String title, boolean deleted) {
        return createAndSaveNote(user, title, "Inhalt...", deleted, false);
    }

    private Note createAndSaveNote(User user, String title, String content, boolean deleted, boolean archived) {
        Note note = Note.builder()
                .title(title)
                .content(content)
                .created(Instant.now())
                .updated(Instant.now())
                .tags(new ArrayList<>())
                .user(user)
                .deleted(deleted)
                .archived(archived)
                .pinned(false)
                .build();

        return noteRepository.saveAndFlush(note);
    }

    private Note createAndSaveNoteWithTags(User user, String title, String content, List<Tag> tags) {
        Note note = Note.builder()
                .title(title)
                .content(content)
                .created(Instant.now())
                .updated(Instant.now())
                .tags(new ArrayList<>(tags))
                .user(user)
                .deleted(false)
                .archived(false)
                .pinned(false)
                .build();

        return noteRepository.saveAndFlush(note);
    }
}
