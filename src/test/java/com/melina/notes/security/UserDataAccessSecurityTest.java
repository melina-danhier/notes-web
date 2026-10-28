package com.melina.notes.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melina.notes.dto.NoteFormDto;
import com.melina.notes.dto.NotePageData;
import com.melina.notes.entity.Note;
import com.melina.notes.entity.User;
import com.melina.notes.repository.NoteRepository;
import com.melina.notes.repository.TagRepository;
import com.melina.notes.repository.UserRepository;
import com.melina.notes.service.NotePageService;
import com.melina.notes.service.NoteService;
import com.melina.notes.service.TagService;
import com.melina.notes.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserDataAccessSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private NoteService noteService;

    @Autowired
    private TagService tagService;

    @BeforeEach
    void setup() {
        cleanAllData();
    }

    @AfterEach
    void tearDown() {
        cleanAllData();
    }

    private void cleanAllData() {
        noteRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Isolation von Notizen und Tags zwischen Benutzern")
    class TagAndNoteIsolationTests {

        private Long userAId;
        private Long userBId;
        private MockHttpSession sessionB;

        @BeforeEach
        void createTwoUsersWithData() throws Exception {
            userService.createUser("Alice", "alice@example.com", "validSecret1");
            User alice = userRepository.findByEmail("alice@example.com").orElseThrow();
            userAId = alice.getId();

            userService.createUser("Bob", "bob@example.com", "validSecret2");
            User bob = userRepository.findByEmail("bob@example.com").orElseThrow();
            userBId = bob.getId();

            noteService.createNoteForUser(userAId,
                    new NoteFormDto("Alice Aktiv", "Inhalt Alice", List.of("privat", "geheim")));
            Long aliceArchivedId = noteService.createNoteForUser(userAId,
                    new NoteFormDto("Alice Archiv", "Inhalt Archiv", List.of("alt"))).getId();
            noteService.archiveNoteForUser(userAId, aliceArchivedId);

            Long aliceTrashId = noteService.createNoteForUser(userAId,
                    new NoteFormDto("Alice Papierkorb", "Inhalt Papierkorb", List.of("trash"))).getId();
            noteService.softDeleteNoteForUser(userAId, aliceTrashId);

            noteService.createNoteForUser(userBId,
                    new NoteFormDto("Bob Aktiv", "Inhalt Bob", List.of("arbeit", "hobby")));

            sessionB = login("bob@example.com", "validSecret2");
        }

        @Test
        @DisplayName("Benutzer B sieht auf der Notizseite nur die eigenen Tags in der Tag-Liste")
        void userSeesOnlyOwnTagsInTagList() throws Exception {
            List<String> tagsUserB = tagService.getAllTagsByUserId(userBId);
            assertThat(tagsUserB).containsExactlyInAnyOrder("arbeit", "hobby");
            assertThat(tagsUserB).doesNotContain("privat", "geheim", "alt", "trash");

            mockMvc.perform(get("/notes").session(sessionB))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("allTags", List.of("arbeit", "hobby")))
                    .andExpect(content().string(containsString("#arbeit")))
                    .andExpect(content().string(containsString("#hobby")))
                    .andExpect(content().string(not(containsString("#privat"))))
                    .andExpect(content().string(not(containsString("#geheim"))));
        }

        @Test
        @DisplayName("Benutzer B sieht weder aktive, archivierte noch gelöschte Notizen von Benutzer A")
        void userCannotSeeOtherUserNotesInAnyView() throws Exception {
            mockMvc.perform(get("/notes").session(sessionB))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Bob Aktiv")))
                    .andExpect(content().string(not(containsString("Alice Aktiv"))));

            mockMvc.perform(get("/notes/archive").session(sessionB))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("Alice Archiv"))));

            mockMvc.perform(get("/notes/trash").session(sessionB))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("Alice Papierkorb"))));
        }

        @Test
        @DisplayName("Benutzer B kann Notiz mit gleichem Tag-Namen anlegen, ohne fremde Daten zu sehen")
        void userCanUseSameTagWithoutDataLeakage() throws Exception {
            noteService.createNoteForUser(userBId,
                    new NoteFormDto("Bob Geheimnis", "Eigenes", List.of("geheim")));

            mockMvc.perform(get("/notes").session(sessionB).param("tagFilter", "geheim"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Bob Geheimnis")))
                    .andExpect(content().string(not(containsString("Alice Aktiv"))));
        }
    }

    @Nested
    @DisplayName("Manipulierte IDs bei Bearbeiten, Archivieren, Wiederherstellen und Löschen")
    class ManipulatedIdTests {

        private Long activeNoteId;
        private Long trashedNoteId;
        private Long archivedNoteId;
        private MockHttpSession session;

        @BeforeEach
        void setupUserAndNotes() throws Exception {
            userService.createUser("Alice", "alice@example.com", "validSecret1");
            User alice = userRepository.findByEmail("alice@example.com").orElseThrow();

            activeNoteId = noteService.createNoteForUser(alice.getId(),
                    new NoteFormDto("Aktiv", "Inhalt", List.of())).getId();

            trashedNoteId = noteService.createNoteForUser(alice.getId(),
                    new NoteFormDto("Papierkorb", "Inhalt", List.of())).getId();
            noteService.softDeleteNoteForUser(alice.getId(), trashedNoteId);

            archivedNoteId = noteService.createNoteForUser(alice.getId(),
                    new NoteFormDto("Archiv", "Inhalt", List.of())).getId();
            noteService.archiveNoteForUser(alice.getId(), archivedNoteId);

            session = login("alice@example.com", "validSecret1");
        }

        @Test
        @DisplayName("Nicht-existente ID (999999) liefert bei allen Operationen 404")
        void nonExistentIdReturnsNotFoundAcrossAllEndpoints() throws Exception {
            long nonExistentId = 999999L;

            mockMvc.perform(get("/notes/{id}", nonExistentId).session(session))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get("/api/notes/{id}", nonExistentId).session(session))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("NOT_FOUND"));

            mockMvc.perform(patch("/api/notes/{id}", nonExistentId)
                            .session(session).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new NoteFormDto("Titel", "Inhalt", List.of()))))
                    .andExpect(status().isNotFound());

            mockMvc.perform(patch("/api/notes/{id}/archive", nonExistentId)
                            .session(session).with(csrf()))
                    .andExpect(status().isNotFound());

            mockMvc.perform(patch("/api/notes/{id}/unarchive", nonExistentId)
                            .session(session).with(csrf()))
                    .andExpect(status().isNotFound());

            mockMvc.perform(patch("/api/notes/{id}/delete", nonExistentId)
                            .session(session).with(csrf()))
                    .andExpect(status().isNotFound());

            mockMvc.perform(patch("/api/notes/{id}/restore", nonExistentId)
                            .session(session).with(csrf()))
                    .andExpect(status().isNotFound());

            mockMvc.perform(delete("/api/notes/{id}", nonExistentId)
                            .session(session).with(csrf()))
                    .andExpect(status().isNotFound());

            mockMvc.perform(patch("/api/notes/{id}/pin", nonExistentId)
                            .session(session).with(csrf()))
                    .andExpect(status().isNotFound());

            mockMvc.perform(patch("/api/notes/{id}/tasks/0/toggle", nonExistentId)
                            .session(session).with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Negative ID (-1) liefert 404 Not Found")
        void negativeIdReturnsNotFound() throws Exception {
            mockMvc.perform(get("/api/notes/-1").session(session))
                    .andExpect(status().isNotFound());

            mockMvc.perform(delete("/api/notes/-1").session(session).with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Nicht-numerische manipulierte ID liefert konsistent 400 Bad Request")
        void nonNumericIdReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/notes/invalid-string-id").session(session))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

            mockMvc.perform(delete("/api/notes/invalid-string-id").session(session).with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("Manipulierter Status: Permanente Löschung einer aktiven Notiz liefert 409 Conflict")
        void deleteActiveNotePermanentlyReturnsConflict() throws Exception {
            mockMvc.perform(delete("/api/notes/{id}", activeNoteId)
                            .session(session).with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value("CONFLICT"));
        }

        @Test
        @DisplayName("Manipulierter Status: Wiederherstellung einer aktiven Notiz liefert 409 Conflict")
        void restoreActiveNoteReturnsConflict() throws Exception {
            mockMvc.perform(patch("/api/notes/{id}/restore", activeNoteId)
                            .session(session).with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value("CONFLICT"));
        }

        @Test
        @DisplayName("Manipulierter Status: Bearbeiten einer gelöschten Notiz liefert 409 Conflict")
        void updateDeletedNoteReturnsConflict() throws Exception {
            NoteFormDto updateDto = new NoteFormDto("Neu", "Inhalt", List.of());

            mockMvc.perform(patch("/api/notes/{id}", trashedNoteId)
                            .session(session).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value("CONFLICT"));
        }

        @Test
        @DisplayName("Manipulierter Status: Anheften einer archivierten Notiz liefert 409 Conflict")
        void pinArchivedNoteReturnsConflict() throws Exception {
            mockMvc.perform(patch("/api/notes/{id}/pin", archivedNoteId)
                            .session(session).with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value("CONFLICT"));
        }

        @Test
        @DisplayName("Manipulierter Status: Erneutes Archivieren einer archivierten Notiz liefert 409 Conflict")
        void archiveAlreadyArchivedNoteReturnsConflict() throws Exception {
            mockMvc.perform(patch("/api/notes/{id}/archive", archivedNoteId)
                            .session(session).with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value("CONFLICT"));
        }
    }

    @Nested
    @DisplayName("Autorisierungsprüfungen für Such-, Filter- und Detailendpunkte")
    class SearchAndFilterAuthorizationTests {

        private Long userAId;
        private Long userANoteId;
        private MockHttpSession sessionB;

        @BeforeEach
        void setupTestData() throws Exception {
            userService.createUser("Alice", "alice@example.com", "validSecret1");
            User alice = userRepository.findByEmail("alice@example.com").orElseThrow();
            userAId = alice.getId();

            userService.createUser("Bob", "bob@example.com", "validSecret2");
            User bob = userRepository.findByEmail("bob@example.com").orElseThrow();

            userANoteId = noteService.createNoteForUser(userAId,
                    new NoteFormDto("Streng geheimer Plan", "Vertraulicher Inhalt", List.of("topsecret"))).getId();

            noteService.createNoteForUser(bob.getId(),
                    new NoteFormDto("Bobs Plan", "Öffentlicher Einkauf", List.of("einkauf")));

            sessionB = login("bob@example.com", "validSecret2");
        }

        @Test
        @DisplayName("Suche nach Stichwort aus fremder Notiz liefert keine Treffer")
        void searchWithKeywordFromOtherUserReturnsNoResults() throws Exception {
            mockMvc.perform(get("/notes").session(sessionB).param("search", "geheimer"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("Streng geheimer Plan"))));
        }

        @Test
        @DisplayName("Suche nach gemeinsamem Begriff liefert ausschließlich eigene Notizen")
        void searchReturnsOnlyOwnMatchingNotes() throws Exception {
            mockMvc.perform(get("/notes").session(sessionB).param("search", "Plan"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Bobs Plan")))
                    .andExpect(content().string(not(containsString("Streng geheimer Plan"))));
        }

        @Test
        @DisplayName("Filter nach fremdem Tag liefert keine Treffer")
        void tagFilterFromOtherUserReturnsNoResults() throws Exception {
            mockMvc.perform(get("/notes").session(sessionB).param("tagFilter", "topsecret"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("Streng geheimer Plan"))));
        }

        @Test
        @DisplayName("Detailaufruf einer fremden Notiz per UI liefert 404 Not Found")
        void detailViewOfOtherUserNoteReturnsNotFound() throws Exception {
            mockMvc.perform(get("/notes/{id}", userANoteId).session(sessionB))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Detailaufruf einer fremden Notiz per REST-API liefert 404 mit Standard-ErrorResponse")
        void detailApiOfOtherUserNoteReturnsNotFoundJson() throws Exception {
            mockMvc.perform(get("/api/notes/{id}", userANoteId).session(sessionB))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value(containsString(String.valueOf(userANoteId))));
        }
    }

    @Nested
    @DisplayName("Konto- und Session-Verhalten nach Logout und Kontolöschung")
    class AccountAndSessionLifecycleTests {

        @Test
        @DisplayName("Session ist nach Logout ungültig und berechtigt zu keinen weiteren Aktionen")
        void sessionIsInvalidatedAfterLogout() throws Exception {
            userService.createUser("Alice", "alice@example.com", "validSecret1");
            MockHttpSession session = login("alice@example.com", "validSecret1");

            mockMvc.perform(get("/notes").session(session))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/logout").session(session).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?logout=true"));

            mockMvc.perform(get("/notes").session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/login"));

            mockMvc.perform(get("/api/notes/1").session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/login"));
        }

        @Test
        @DisplayName("Kontolöschung invalidiert die Session, entfernt Benutzer, Notizen und verwaiste Tags vollständig")
        void deleteAccountPurgesDataAndInvalidatesSession() throws Exception {
            userService.createUser("Alice", "alice@example.com", "validSecret1");
            User alice = userRepository.findByEmail("alice@example.com").orElseThrow();
            Long aliceId = alice.getId();

            userService.createUser("Bob", "bob@example.com", "validSecret2");
            User bob = userRepository.findByEmail("bob@example.com").orElseThrow();
            Long bobId = bob.getId();

            noteService.createNoteForUser(aliceId,
                    new NoteFormDto("Alice Notiz 1", "Inhalt 1", List.of("alice-tag")));
            noteService.createNoteForUser(aliceId,
                    new NoteFormDto("Alice Notiz 2", "Inhalt 2", List.of("gemeinsam")));
            noteService.createNoteForUser(bobId,
                    new NoteFormDto("Bob Notiz", "Inhalt Bob", List.of("gemeinsam", "bob-tag")));

            MockHttpSession aliceSession = login("alice@example.com", "validSecret1");

            mockMvc.perform(post("/settings/delete")
                            .session(aliceSession)
                            .with(csrf())
                            .param("password", "validSecret1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?accountDeleted=true"));

            // 1. Session ist invalidiert
            mockMvc.perform(get("/notes").session(aliceSession))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/login"));

            // 2. Benutzer und Notizen von Alice sind aus der DB gelöscht
            assertThat(userRepository.findById(aliceId)).isEmpty();
            assertThat(noteRepository.findAllByUserId(aliceId)).isEmpty();

            // 3. Verwaistes Tag "alice-tag" wurde aufgeräumt, "gemeinsam" und "bob-tag" bleiben für Bob
            List<String> bobTags = tagService.getAllTagsByUserId(bobId);
            assertThat(bobTags).containsExactlyInAnyOrder("gemeinsam", "bob-tag");
            assertThat(tagRepository.findByTag("alice-tag")).isEmpty();
            assertThat(tagRepository.findByTag("gemeinsam")).isPresent();

            // 4. Bobs Notizen sind vollständig intakt
            List<Note> bobNotes = noteRepository.findAllByUserId(bobId);
            assertThat(bobNotes).hasSize(1);
            assertThat(bobNotes.get(0).getTitle()).isEqualTo("Bob Notiz");

            // 5. Erneuter Login mit gelöschten Zugangsdaten scheitert
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("email", "alice@example.com")
                            .param("password", "validSecret1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error=true"))
                    .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("Kontolöschung mit falschem Passwort schlägt fehl, Daten und Session bleiben erhalten")
        void deleteAccountWithWrongPasswordFails() throws Exception {
            userService.createUser("Alice", "alice@example.com", "validSecret1");
            User alice = userRepository.findByEmail("alice@example.com").orElseThrow();
            MockHttpSession session = login("alice@example.com", "validSecret1");

            mockMvc.perform(post("/settings/delete")
                            .session(session)
                            .with(csrf())
                            .param("password", "falschesPasswort"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("deleteForm", "password"));

            assertThat(userRepository.findById(alice.getId())).isPresent();

            mockMvc.perform(get("/notes").session(session))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Konsistente Fehlerantworten bei unzulässigen Zugriffen")
    class ConsistentErrorResponsesTests {

        private MockHttpSession session;

        @BeforeEach
        void setupUser() throws Exception {
            userService.createUser("Alice", "alice@example.com", "validSecret1");
            session = login("alice@example.com", "validSecret1");
        }

        @Test
        @DisplayName("Validierungsfehler liefern Status 400 mit Fehlermeldung und Details")
        void validationErrorsReturnStructured400() throws Exception {
            NoteFormDto invalidDto = new NoteFormDto("Zu langer Titel ".repeat(5), "Inhalt", List.of());

            mockMvc.perform(post("/api/notes")
                            .session(session)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message").value("Validierungsfehler"))
                    .andExpect(jsonPath("$.details").isArray());
        }

        @Test
        @DisplayName("Ungültiger Request-Body liefert Status 400 mit einheitlicher Struktur")
        void malformedJsonReturnsStructured400() throws Exception {
            mockMvc.perform(post("/api/notes")
                            .session(session)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{nicht-valides-json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.message").value("Ungültiger Request-Body"));
        }

        @Test
        @DisplayName("Nicht unterstützte HTTP-Methode liefert Status 405 Method Not Allowed")
        void unsupportedMethodReturns405() throws Exception {
            mockMvc.perform(post("/api/notes/1")
                            .session(session)
                            .with(csrf()))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status").value("METHOD_NOT_ALLOWED"));
        }
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notes"))
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
