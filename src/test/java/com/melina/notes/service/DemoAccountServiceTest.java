package com.melina.notes.service;

import com.melina.notes.entity.Note;
import com.melina.notes.entity.Tag;
import com.melina.notes.entity.User;
import com.melina.notes.exception.DemoCapacityExceededException;
import com.melina.notes.repository.NoteRepository;
import com.melina.notes.repository.TagRepository;
import com.melina.notes.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class DemoAccountServiceTest {

    @Autowired
    private DemoAccountService demoAccountService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TagService tagService;

    @BeforeEach
    void setup() {
        cleanAllData();
    }

    @AfterEach
    void tearDown() {
        cleanAllData();
        ReflectionTestUtils.setField(demoAccountService, "maxAccounts", 50);
    }

    private void cleanAllData() {
        noteRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Erstellung und Lebenszyklus temporärer Demo-Konten")
    class LifecycleAndCreationTests {

        @Test
        @DisplayName("createDemoAccount initialisiert Demo-Nutzer mit Rolle, Ablaufdatum und 5 Notizen")
        void createDemoAccountInitializesUserAndNotes() {
            Instant before = Instant.now();
            DemoAccountService.DemoCredentials credentials = demoAccountService.createDemoAccount();
            Instant after = Instant.now();

            assertThat(credentials.userId()).isNotNull();
            assertThat(credentials.email()).startsWith("demo-").endsWith("@example.invalid");
            assertThat(credentials.password()).isNotBlank();

            User user = userRepository.findById(credentials.userId()).orElseThrow();
            assertThat(user.isDemoAccount()).isTrue();
            assertThat(user.getRole()).isEqualTo("ROLE_DEMO");
            assertThat(user.getDisplayName()).isEqualTo("Demo User");
            assertThat(user.getExpiresAt()).isBetween(
                    before.plus(Duration.ofHours(2)).minusSeconds(5),
                    after.plus(Duration.ofHours(2)).plusSeconds(5)
            );

            List<Note> notes = noteRepository.findAllByUserId(user.getId());
            assertThat(notes).hasSize(5);

            long pinnedCount = notes.stream().filter(Note::isPinned).count();
            long archivedCount = notes.stream().filter(Note::isArchived).count();
            long deletedCount = notes.stream().filter(Note::isDeleted).count();
            assertThat(pinnedCount).isEqualTo(1);
            assertThat(archivedCount).isEqualTo(1);
            assertThat(deletedCount).isEqualTo(1);

            List<String> tagNames = tagService.getAllTagsByUserId(user.getId());
            assertThat(tagNames).contains("start", "markdown", "privat", "ideen");
        }
    }

    @Nested
    @DisplayName("Bereinigung abgelaufener Demo-Konten")
    class CleanupExpiredAccountsTests {

        @Test
        @DisplayName("deleteExpiredDemoAccounts bereinigt nur abgelaufene Konten und erhält aktive Konten")
        void deletesOnlyExpiredAccounts() {
            // 1. Abgelaufener Demo-Account
            DemoAccountService.DemoCredentials expiredCreds = demoAccountService.createDemoAccount();
            User expiredUser = userRepository.findById(expiredCreds.userId()).orElseThrow();
            expiredUser.setExpiresAt(Instant.now().minusSeconds(10));
            userRepository.saveAndFlush(expiredUser);

            // 2. Noch aktiver Demo-Account
            DemoAccountService.DemoCredentials activeCreds = demoAccountService.createDemoAccount();

            // Ausführen
            demoAccountService.deleteExpiredDemoAccounts();

            // Prüfen: Abgelaufener Account und Notizen sind weg
            assertThat(userRepository.findById(expiredCreds.userId())).isEmpty();
            assertThat(noteRepository.findAllByUserId(expiredCreds.userId())).isEmpty();

            // Prüfen: Aktiver Account und Notizen bleiben erhalten
            assertThat(userRepository.findById(activeCreds.userId())).isPresent();
            assertThat(noteRepository.findAllByUserId(activeCreds.userId())).hasSize(5);
        }

        @Test
        @DisplayName("Wiederholter Aufruf von deleteExpiredDemoAccounts ist idempotent")
        void repeatedCleanupIsIdempotent() {
            DemoAccountService.DemoCredentials expiredCreds = demoAccountService.createDemoAccount();
            User expiredUser = userRepository.findById(expiredCreds.userId()).orElseThrow();
            expiredUser.setExpiresAt(Instant.now().minusSeconds(10));
            userRepository.saveAndFlush(expiredUser);

            // Erster Aufruf
            demoAccountService.deleteExpiredDemoAccounts();
            assertThat(userRepository.findById(expiredCreds.userId())).isEmpty();

            // Zweiter Aufruf direkt danach darf keine Fehler werfen
            demoAccountService.deleteExpiredDemoAccounts();
            assertThat(userRepository.findById(expiredCreds.userId())).isEmpty();
        }

        @Test
        @DisplayName("deleteDemoAccount ist idempotent und gibt beim zweiten Aufruf false zurück")
        void deleteDemoAccountIsIdempotent() {
            DemoAccountService.DemoCredentials creds = demoAccountService.createDemoAccount();

            boolean firstCall = demoAccountService.deleteDemoAccount(creds.userId());
            boolean secondCall = demoAccountService.deleteDemoAccount(creds.userId());

            assertThat(firstCall).isTrue();
            assertThat(secondCall).isFalse();
            assertThat(userRepository.findById(creds.userId())).isEmpty();
        }

        @Test
        @DisplayName("Mehrere abgelaufene Konten werden in einem Durchlauf bereinigt")
        void deletesMultipleExpiredAccounts() {
            List<Long> expiredIds = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                DemoAccountService.DemoCredentials creds = demoAccountService.createDemoAccount();
                User user = userRepository.findById(creds.userId()).orElseThrow();
                user.setExpiresAt(Instant.now().minusSeconds(10));
                userRepository.saveAndFlush(user);
                expiredIds.add(creds.userId());
            }

            demoAccountService.deleteExpiredDemoAccounts();

            for (Long id : expiredIds) {
                assertThat(userRepository.findById(id)).isEmpty();
                assertThat(noteRepository.findAllByUserId(id)).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Schutz regulärer Konten und fremder Daten")
    class ProtectionOfRegularAccountsTests {

        @Test
        @DisplayName("deleteExpiredDemoAccounts löscht niemals reguläre Benutzerkonten, selbst mit vergangener Zeit")
        void neverDeletesRegularUserAccounts() {
            User regularUser = userRepository.saveAndFlush(User.builder()
                    .displayName("Echter Benutzer")
                    .email("real-user@example.com")
                    .password("encoded")
                    .role("ROLE_USER")
                    .demoAccount(false)
                    .expiresAt(Instant.now().minusSeconds(100))
                    .build());

            Note note = noteRepository.saveAndFlush(Note.builder()
                    .user(regularUser)
                    .title("Wichtige Notiz")
                    .content("Darf nicht gelöscht werden")
                    .created(Instant.now())
                    .updated(Instant.now())
                    .tags(List.of())
                    .build());

            demoAccountService.deleteExpiredDemoAccounts();

            assertThat(userRepository.findById(regularUser.getId())).isPresent();
            assertThat(noteRepository.findById(note.getId())).isPresent();
        }

        @Test
        @DisplayName("deleteDemoAccount lehnt das Löschen regulärer Benutzer ab und gibt false zurück")
        void deleteDemoAccountRejectsRegularUser() {
            User regularUser = userRepository.saveAndFlush(User.builder()
                    .displayName("Echter Benutzer")
                    .email("real-user@example.com")
                    .password("encoded")
                    .role("ROLE_USER")
                    .demoAccount(false)
                    .build());

            boolean result = demoAccountService.deleteDemoAccount(regularUser.getId());

            assertThat(result).isFalse();
            assertThat(userRepository.findById(regularUser.getId())).isPresent();
        }

        @Test
        @DisplayName("Löschen eines Demo-Kontos erhält Tags, die von regulären Benutzern verwendet werden")
        void keepsTagsUsedByRegularUsers() {
            Tag sharedTag = tagRepository.saveAndFlush(Tag.builder().tag("gemeinsam").build());

            User regularUser = userRepository.saveAndFlush(User.builder()
                    .displayName("Regulär")
                    .email("regular@example.com")
                    .password("encoded")
                    .role("ROLE_USER")
                    .demoAccount(false)
                    .build());

            noteRepository.saveAndFlush(Note.builder()
                    .user(regularUser)
                    .title("Reguläre Notiz")
                    .content("Inhalt")
                    .created(Instant.now())
                    .updated(Instant.now())
                    .tags(List.of(sharedTag))
                    .build());

            DemoAccountService.DemoCredentials demoCreds = demoAccountService.createDemoAccount();
            User demoUser = userRepository.findById(demoCreds.userId()).orElseThrow();

            noteRepository.saveAndFlush(Note.builder()
                    .user(demoUser)
                    .title("Demo Notiz mit geteiltem Tag")
                    .content("Inhalt")
                    .created(Instant.now())
                    .updated(Instant.now())
                    .tags(List.of(sharedTag))
                    .build());

            demoAccountService.deleteDemoAccount(demoUser.getId());

            assertThat(tagRepository.findByTag("gemeinsam")).isPresent();
            assertThat(noteRepository.findAllByUserId(regularUser.getId())).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Kapazitätsgrenze und Cleanup vor Neuanlage")
    class CapacityAndCleanupBeforeCreationTests {

        @Test
        @DisplayName("createDemoAccount wirft DemoCapacityExceededException wenn Limit erreicht ist")
        void throwsExceptionWhenCapacityReached() {
            ReflectionTestUtils.setField(demoAccountService, "maxAccounts", 2);

            demoAccountService.createDemoAccount();
            demoAccountService.createDemoAccount();

            assertThatThrownBy(() -> demoAccountService.createDemoAccount())
                    .isInstanceOf(DemoCapacityExceededException.class)
                    .hasMessageContaining("2");
        }

        @Test
        @DisplayName("createDemoAccount räumt abgelaufene Konten vor Kapazitätsprüfung auf")
        void cleansExpiredBeforeCheckingCapacity() {
            ReflectionTestUtils.setField(demoAccountService, "maxAccounts", 2);

            DemoAccountService.DemoCredentials creds1 = demoAccountService.createDemoAccount();
            demoAccountService.createDemoAccount();

            // Account 1 abgelaufen markieren
            User user1 = userRepository.findById(creds1.userId()).orElseThrow();
            user1.setExpiresAt(Instant.now().minusSeconds(1));
            userRepository.saveAndFlush(user1);

            // Neuanlage sollte gelingen, da Account 1 automatisch aufgeräumt wird
            DemoAccountService.DemoCredentials newCreds = demoAccountService.createDemoAccount();

            assertThat(newCreds).isNotNull();
            assertThat(userRepository.findById(creds1.userId())).isEmpty();
            assertThat(userRepository.countByDemoAccountTrue()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Parallele Demo-Anfragen (Concurrency)")
    class ConcurrencyTests {

        @Test
        @DisplayName("Gleichzeitige Demo-Konto-Erstellungen erzeugen isolierte Konten ohne Kollision")
        void concurrentDemoCreationsSucceedWithoutCollision() throws InterruptedException {
            int threadCount = 8;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch endGate = new CountDownLatch(threadCount);

            Set<Long> userIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
            Set<String> emails = Collections.newSetFromMap(new ConcurrentHashMap<>());
            List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startGate.await();
                        DemoAccountService.DemoCredentials creds = demoAccountService.createDemoAccount();
                        userIds.add(creds.userId());
                        emails.add(creds.email());
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        endGate.countDown();
                    }
                });
            }

            startGate.countDown();
            boolean finished = endGate.await(15, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(finished).isTrue();
            assertThat(errors).isEmpty();
            assertThat(userIds).hasSize(threadCount);
            assertThat(emails).hasSize(threadCount);
            assertThat(noteRepository.count()).isEqualTo(threadCount * 5L);
        }
    }
}
