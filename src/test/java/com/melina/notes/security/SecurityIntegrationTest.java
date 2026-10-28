package com.melina.notes.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melina.notes.dto.NoteFormDto;
import com.melina.notes.entity.Note;
import com.melina.notes.entity.User;
import com.melina.notes.repository.NoteRepository;
import com.melina.notes.repository.TagRepository;
import com.melina.notes.repository.UserRepository;
import com.melina.notes.service.DemoAccountService;
import com.melina.notes.service.NoteService;
import com.melina.notes.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

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
    private DemoAccountService demoAccountService;

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
    @DisplayName("Anonymer Zugriff und öffentliche Endpunkte")
    class AnonymousAccessTests {

        @Test
        @DisplayName("Geschützte Seiten leiten unauthentifizierte Anfragen auf /login um")
        void protectedPagesRedirectAnonymousUsersToLogin() throws Exception {
            String[] protectedUrls = {
                    "/notes",
                    "/notes/new",
                    "/notes/1",
                    "/notes/archive",
                    "/notes/trash",
                    "/settings"
            };

            for (String url : protectedUrls) {
                mockMvc.perform(get(url))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("http://localhost/login"));
            }
        }

        @Test
        @DisplayName("Geschützte API-Endpunkte fordern Authentifizierung an")
        void protectedApiEndpointsRequireAuthentication() throws Exception {
            mockMvc.perform(get("/api/notes/1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/login"));

            mockMvc.perform(post("/api/notes")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Test\",\"content\":\"Inhalt\"}"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/login"));
        }

        @Test
        @DisplayName("Öffentliche Endpunkte sind ohne Authentifizierung erreichbar")
        void publicEndpointsAreAccessibleAnonymously() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/signup"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Authentifizierung, Login und Logout")
    class AuthenticationAndSessionTests {

        @Test
        @DisplayName("Login mit gültigen Zugangsdaten erstellt authentifizierte Session und leitet zu /notes")
        void loginWithValidCredentialsAuthenticates() throws Exception {
            userService.createUser("Alice", "alice@example.com", "validSecret1");

            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("email", "alice@example.com")
                            .param("password", "validSecret1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/notes"))
                    .andExpect(authenticated().withUsername("alice@example.com"));
        }

        @Test
        @DisplayName("Login mit falschem Passwort schlägt fehl und leitet mit error=true um")
        void loginWithWrongPasswordFails() throws Exception {
            userService.createUser("Alice", "alice@example.com", "validSecret1");

            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("email", "alice@example.com")
                            .param("password", "wrongSecret1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error=true"))
                    .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("Login mit nicht registrierter E-Mail schlägt fehl und leitet mit error=true um")
        void loginWithNonExistentUserFails() throws Exception {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("email", "nobody@example.com")
                            .param("password", "validSecret1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error=true"))
                    .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("Logout invalidiert die Session und Folgezugriffe werden umgeleitet")
        void logoutInvalidatesSession() throws Exception {
            userService.createUser("Alice", "alice@example.com", "validSecret1");
            MockHttpSession session = login("alice@example.com", "validSecret1");

            mockMvc.perform(post("/logout")
                            .session(session)
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?logout=true"));

            mockMvc.perform(get("/notes").session(session))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("http://localhost/login"));
        }
    }

    @Nested
    @DisplayName("CSRF-Schutz")
    class CsrfProtectionTests {

        @Test
        @DisplayName("Zustandsändernde Requests ohne CSRF-Token werden mit 403 abgewiesen")
        void stateChangingRequestsWithoutCsrfReturnForbidden() throws Exception {
            mockMvc.perform(post("/login")
                            .param("email", "alice@example.com")
                            .param("password", "validSecret1"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/logout"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/signup")
                            .param("name", "Alice")
                            .param("email", "alice@example.com")
                            .param("password", "validSecret1")
                            .param("confirmPassword", "validSecret1"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/demo-login"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Authentifizierte API- und Settings-Aktionen ohne CSRF-Token werden mit 403 abgewiesen")
        void authenticatedActionsWithoutCsrfReturnForbidden() throws Exception {
            userService.createUser("Alice", "alice@example.com", "validSecret1");
            MockHttpSession session = login("alice@example.com", "validSecret1");

            mockMvc.perform(post("/settings/name")
                            .session(session)
                            .param("name", "Neuer Name"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/settings/password")
                            .session(session)
                            .param("currentPassword", "validSecret1")
                            .param("newPassword", "newValidSecret1")
                            .param("confirmPassword", "newValidSecret1"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/settings/delete")
                            .session(session)
                            .param("password", "validSecret1"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/notes")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Ohne CSRF\",\"content\":\"Test\"}"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(patch("/api/notes/1")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Update\",\"content\":\"Test\"}"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(delete("/api/notes/1")
                            .session(session))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Mandantentrennung und unberechtigter Zugriff (IDOR-Prävention)")
    class AuthorizationAndMultiTenancyTests {

        private Long userAId;
        private Long userBId;
        private Long userANoteId;
        private Long userATrashNoteId;
        private MockHttpSession userBSession;

        @BeforeEach
        void createTestData() throws Exception {
            userService.createUser("Alice", "alice@example.com", "validSecret1");
            User alice = userRepository.findByEmail("alice@example.com").orElseThrow();
            userAId = alice.getId();

            userService.createUser("Bob", "bob@example.com", "validSecret2");
            User bob = userRepository.findByEmail("bob@example.com").orElseThrow();
            userBId = bob.getId();

            userANoteId = noteService.createNoteForUser(userAId,
                    new NoteFormDto("Alice Notiz", "- [ ] Aufgabe", List.of("privat"))).getId();

            userATrashNoteId = noteService.createNoteForUser(userAId,
                    new NoteFormDto("Alice Papierkorb", "Trash Inhalt", List.of())).getId();
            noteService.softDeleteNoteForUser(userAId, userATrashNoteId);

            userBSession = login("bob@example.com", "validSecret2");
        }

        @Test
        @DisplayName("Benutzer B kann fremde Notiz über API nicht lesen (404)")
        void userCannotReadOtherUserNote() throws Exception {
            mockMvc.perform(get("/api/notes/{id}", userANoteId)
                            .session(userBSession))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("Benutzer B kann fremde Notiz über API nicht aktualisieren (404)")
        void userCannotUpdateOtherUserNote() throws Exception {
            NoteFormDto updateDto = new NoteFormDto("Gehackt", "Inhalt", List.of());

            mockMvc.perform(patch("/api/notes/{id}", userANoteId)
                            .session(userBSession)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isNotFound());

            Note aliceNote = noteRepository.findById(userANoteId).orElseThrow();
            assertThat(aliceNote.getTitle()).isEqualTo("Alice Notiz");
        }

        @Test
        @DisplayName("Benutzer B kann fremde Notiz nicht in Papierkorb verschieben (404)")
        void userCannotSoftDeleteOtherUserNote() throws Exception {
            mockMvc.perform(patch("/api/notes/{id}/delete", userANoteId)
                            .session(userBSession)
                            .with(csrf()))
                    .andExpect(status().isNotFound());

            Note aliceNote = noteRepository.findById(userANoteId).orElseThrow();
            assertThat(aliceNote.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("Benutzer B kann fremde gelöschte Notiz nicht wiederherstellen (404)")
        void userCannotRestoreOtherUserNote() throws Exception {
            mockMvc.perform(patch("/api/notes/{id}/restore", userATrashNoteId)
                            .session(userBSession)
                            .with(csrf()))
                    .andExpect(status().isNotFound());

            Note aliceNote = noteRepository.findById(userATrashNoteId).orElseThrow();
            assertThat(aliceNote.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("Benutzer B kann fremde Notiz nicht endgültig löschen (404)")
        void userCannotPermanentlyDeleteOtherUserNote() throws Exception {
            mockMvc.perform(delete("/api/notes/{id}", userATrashNoteId)
                            .session(userBSession)
                            .with(csrf()))
                    .andExpect(status().isNotFound());

            assertThat(noteRepository.findById(userATrashNoteId)).isPresent();
        }

        @Test
        @DisplayName("Benutzer B kann fremde Notiz nicht archivieren oder anheften (404)")
        void userCannotArchiveOrPinOtherUserNote() throws Exception {
            mockMvc.perform(patch("/api/notes/{id}/archive", userANoteId)
                            .session(userBSession)
                            .with(csrf()))
                    .andExpect(status().isNotFound());

            mockMvc.perform(patch("/api/notes/{id}/pin", userANoteId)
                            .session(userBSession)
                            .with(csrf()))
                    .andExpect(status().isNotFound());

            Note aliceNote = noteRepository.findById(userANoteId).orElseThrow();
            assertThat(aliceNote.isArchived()).isFalse();
            assertThat(aliceNote.isPinned()).isFalse();
        }

        @Test
        @DisplayName("Benutzer B kann Checkbox in fremder Notiz nicht toggeln (404)")
        void userCannotToggleTaskInOtherUserNote() throws Exception {
            mockMvc.perform(patch("/api/notes/{id}/tasks/0/toggle", userANoteId)
                            .session(userBSession)
                            .with(csrf()))
                    .andExpect(status().isNotFound());

            Note aliceNote = noteRepository.findById(userANoteId).orElseThrow();
            assertThat(aliceNote.getContent()).contains("- [ ] Aufgabe");
        }

        @Test
        @DisplayName("Papierkorb leeren von Benutzer B löscht keine Notizen von Benutzer A")
        void emptyTrashDoesNotDeleteOtherUserNotes() throws Exception {
            mockMvc.perform(delete("/api/notes/trash/empty")
                            .session(userBSession)
                            .with(csrf()))
                    .andExpect(status().isOk());

            assertThat(noteRepository.findById(userATrashNoteId)).isPresent();
        }

        @Test
        @DisplayName("Benutzer B kann fremde Notiz im Web-UI nicht aufrufen (404)")
        void userCannotViewOtherUserNoteInWebUi() throws Exception {
            mockMvc.perform(get("/notes/{id}", userANoteId)
                            .session(userBSession))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Demo-Konto-Berechtigungen und Absicherung")
    class DemoAccountPrivilegeTests {

        @Test
        @DisplayName("Demo-Benutzer kann Passwort nicht ändern (403 Forbidden)")
        void demoUserCannotChangePassword() throws Exception {
            MvcResult loginResult = mockMvc.perform(post("/demo-login").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andReturn();
            MockHttpSession demoSession = (MockHttpSession) loginResult.getRequest().getSession(false);

            mockMvc.perform(post("/settings/password")
                            .session(demoSession)
                            .with(csrf())
                            .param("currentPassword", "anyPassword")
                            .param("newPassword", "newValidSecret1")
                            .param("confirmPassword", "newValidSecret1"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("Demo-Benutzer kann Konto nicht über Settings löschen (403 Forbidden)")
        void demoUserCannotDeleteAccountViaSettings() throws Exception {
            MvcResult loginResult = mockMvc.perform(post("/demo-login").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andReturn();
            MockHttpSession demoSession = (MockHttpSession) loginResult.getRequest().getSession(false);

            mockMvc.perform(post("/settings/delete")
                            .session(demoSession)
                            .with(csrf())
                            .param("password", "anyPassword"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value("FORBIDDEN"));
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
