package com.melina.notes.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melina.notes.entity.Note;
import com.melina.notes.repository.NoteRepository;
import com.melina.notes.repository.TagRepository;
import com.melina.notes.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NoteWorkflowsE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        noteRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void userCanRegisterLoginCreateUpdateAndFilterNote() throws Exception {
        register("Melina", " MELINA@example.com ", "secret1");
        MockHttpSession session = login("melina@example.com", "secret1");

        long noteId = createNote(session, "Einkauf", "Milch und Brot", "Privat", " privat ");

        mockMvc.perform(get("/notes")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Einkauf")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("#privat")));

        mockMvc.perform(patch("/api/notes/{noteId}", noteId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noteJson("Wochenende", "Kuchen backen", "privat", "planung")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(noteId))
                .andExpect(jsonPath("$.title").value("Wochenende"))
                .andExpect(jsonPath("$.tags.length()").value(2));

        mockMvc.perform(get("/notes")
                        .session(session)
                        .param("search", "kuchen")
                        .param("tagFilter", "planung"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Wochenende")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Einkauf"))));

        Note savedNote = noteRepository.findById(noteId).orElseThrow();
        assertThat(savedNote.getTitle()).isEqualTo("Wochenende");
        assertThat(tagRepository.findAll())
                .extracting("tag")
                .containsExactlyInAnyOrder("privat", "planung");
    }

    @Test
    void userCanMoveNotesToTrashRestoreOneAndEmptyTheRest() throws Exception {
        register("Melina", "melina@example.com", "secret1");
        MockHttpSession session = login("melina@example.com", "secret1");
        long restoredNoteId = createNote(session, "Behalten", "Wichtiger Inhalt", "wichtig");
        long deletedNoteId = createNote(session, "Entfernen", "Alter Inhalt", "alt");

        softDelete(session, restoredNoteId);
        softDelete(session, deletedNoteId);

        mockMvc.perform(get("/notes/trash").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Behalten")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Entfernen")));

        mockMvc.perform(patch("/api/notes/{noteId}/restore", restoredNoteId)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/notes/trash/empty")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk());

        assertThat(noteRepository.findById(restoredNoteId))
                .get()
                .extracting(Note::isDeleted)
                .isEqualTo(false);
        assertThat(noteRepository.findById(deletedNoteId)).isEmpty();

        mockMvc.perform(get("/notes").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Behalten")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Entfernen"))));
    }

    @Test
    void authenticationCsrfAndOwnershipAreEnforced() throws Exception {
        mockMvc.perform(get("/notes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        register("Melina", "melina@example.com", "secret1");
        MockHttpSession ownerSession = login("melina@example.com", "secret1");
        long noteId = createNote(ownerSession, "Privat", "Nur fuer mich", "privat");

        mockMvc.perform(patch("/api/notes/{noteId}/delete", noteId)
                        .session(ownerSession))
                .andExpect(status().isForbidden());

        register("Andere Person", "other@example.com", "secret2");
        MockHttpSession otherSession = login("other@example.com", "secret2");

        mockMvc.perform(get("/api/notes/{noteId}", noteId)
                        .session(otherSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"));

        assertThat(noteRepository.findById(noteId)).isPresent();
    }

    private void register(String name, String email, String password) throws Exception {
        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("name", name)
                        .param("email", email)
                        .param("password", password)
                        .param("confirmPassword", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));
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

    private long createNote(MockHttpSession session, String title, String content, String... tags)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/notes")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noteJson(title, content, tags)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asLong();
    }

    private void softDelete(MockHttpSession session, long noteId) throws Exception {
        mockMvc.perform(patch("/api/notes/{noteId}/delete", noteId)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(noteId));
    }

    private String noteJson(String title, String content, String... tags) throws Exception {
        return objectMapper.writeValueAsString(new NoteRequest(title, content, tags));
    }

    private record NoteRequest(String title, String content, String[] tags) {
    }
}
