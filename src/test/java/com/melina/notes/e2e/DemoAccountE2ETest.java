package com.melina.notes.e2e;

import com.melina.notes.entity.Note;
import com.melina.notes.entity.User;
import com.melina.notes.repository.NoteRepository;
import com.melina.notes.repository.UserRepository;
import com.melina.notes.security.CustomUserDetails;
import com.melina.notes.service.DemoAccountService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoAccountE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoAccountService demoAccountService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    @AfterEach
    void removeRemainingDemoAccounts() {
        userRepository.findAll().stream()
                .filter(User::isDemoAccount)
                .map(User::getId)
                .toList()
                .forEach(demoAccountService::deleteDemoAccount);
    }

    @Test
    void demoLoginCreatesIsolatedDataAndLogoutDeletesIt() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/demo-login").with(csrf()))
                .andExpectAll(
                        status().is3xxRedirection(),
                        redirectedUrl("/notes"),
                        authenticated().withRoles("DEMO")
                )
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        CustomUserDetails principal = principalFrom(session);

        assertThat(userRepository.findById(principal.getId())).isPresent();
        List<Note> demoNotes = noteRepository.findAllByUserId(principal.getId());
        assertThat(demoNotes).hasSize(5);
        assertThat(demoNotes)
                .extracting(Note::getContent)
                .anyMatch(content -> content.contains("- [ ] Eigene Notiz erstellen"))
                .anyMatch(content -> content.contains(":::toggle Tipp zu Aufgaben")
                        && content.contains("\n:::\n"))
                .anyMatch(content -> content.contains("**Diese Zeile ist fett**"))
                .noneMatch(content -> content.contains(">;"));

        mockMvc.perform(get("/notes").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpectAll(
                        status().is3xxRedirection(),
                        redirectedUrl("/login?logout=true")
                );

        assertThat(userRepository.findById(principal.getId())).isEmpty();
        assertThat(noteRepository.findAllByUserId(principal.getId())).isEmpty();
    }

    @Test
    void cleanupDeletesExpiredDemoAccount() {
        DemoAccountService.DemoCredentials credentials = demoAccountService.createDemoAccount();
        User user = userRepository.findById(credentials.userId()).orElseThrow();
        user.setExpiresAt(Instant.now().minusSeconds(1));
        userRepository.saveAndFlush(user);

        demoAccountService.deleteExpiredDemoAccounts();

        assertThat(userRepository.findById(credentials.userId())).isEmpty();
        assertThat(noteRepository.findAllByUserId(credentials.userId())).isEmpty();
    }

    @Test
    void cleanupServiceNeverDeletesRegularAccount() {
        User regularUser = userRepository.saveAndFlush(User.builder()
                .displayName("Regular User")
                .email("regular-demo-test@example.com")
                .password("encoded")
                .role("ROLE_USER")
                .build());

        boolean deleted = demoAccountService.deleteDemoAccount(regularUser.getId());

        assertThat(deleted).isFalse();
        assertThat(userRepository.findById(regularUser.getId())).isPresent();
        userRepository.delete(regularUser);
    }

    private CustomUserDetails principalFrom(HttpSession session) {
        SecurityContext context = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        return (CustomUserDetails) context.getAuthentication().getPrincipal();
    }
}
