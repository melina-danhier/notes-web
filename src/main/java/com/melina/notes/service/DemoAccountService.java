package com.melina.notes.service;

import com.melina.notes.entity.Note;
import com.melina.notes.entity.Tag;
import com.melina.notes.entity.User;
import com.melina.notes.exception.DemoCapacityExceededException;
import com.melina.notes.repository.NoteRepository;
import com.melina.notes.repository.TagRepository;
import com.melina.notes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoAccountService {

    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final TagRepository tagRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo.account-lifetime:PT2H}")
    private Duration accountLifetime;

    @Value("${app.demo.max-accounts:50}")
    private int maxAccounts;

    @Transactional(noRollbackFor = DemoCapacityExceededException.class)
    public DemoCredentials createDemoAccount() {
        deleteExpiredDemoAccounts();

        if (userRepository.countByDemoAccountTrue() >= maxAccounts) {
            throw new DemoCapacityExceededException(maxAccounts);
        }

        String token = UUID.randomUUID().toString();
        String email = "demo-" + token + "@example.invalid";
        String password = UUID.randomUUID().toString();
        Instant now = Instant.now();

        User user = userRepository.save(User.builder()
                .displayName("Demo User")
                .email(email)
                .password(passwordEncoder.encode(password))
                .role("ROLE_DEMO")
                .demoAccount(true)
                .expiresAt(now.plus(accountLifetime))
                .build());

        Tag start = findOrCreateTag("start");
        Tag markdown = findOrCreateTag("markdown");
        Tag privat = findOrCreateTag("privat");
        Tag ideen = findOrCreateTag("ideen");

        noteRepository.saveAll(List.of(
                note(user, "Willkommen bei Notes 👋", """
                        # Deine persönliche Demo ist bereit

                        Du kannst hier alles ausprobieren. Deine Änderungen sind nur für dich sichtbar
                        und werden nach dem Logout oder automatisch nach einiger Zeit gelöscht.

                        - Notizen mit **Markdown** formatieren
                        - Tags vergeben
                        - wichtige Notizen anheften
                        - Inhalte suchen, archivieren oder löschen
                        """, now.minus(4, ChronoUnit.DAYS), List.of(start, markdown), true, false, false),
                note(user, "Markdown kurz ausprobieren", """
                        ## Kleine Checkliste

                        - [x] Demo öffnen
                        - [ ] Eigene Notiz erstellen
                        - [ ] Einen Tag hinzufügen

                        Du kannst außerdem `Code`, **fetten Text** und Links verwenden.
                        """, now.minus(3, ChronoUnit.DAYS), List.of(markdown), false, false, false),
                note(user, "Ideen für das Wochenende", """
                        - Fahrradtour planen
                        - Neues Rezept ausprobieren
                        - Fotos sortieren
                        """, now.minus(2, ChronoUnit.DAYS), List.of(privat, ideen), false, false, false),
                note(user, "Archivierte Beispielnotiz",
                        "Diese Notiz liegt bereits im Archiv und demonstriert den Archiv-Workflow.",
                        now.minus(1, ChronoUnit.DAYS), List.of(start), false, true, false),
                note(user, "Gelöschte Beispielnotiz",
                        "Diese Notiz befindet sich im Papierkorb und kann wiederhergestellt werden.",
                        now, List.of(start), false, false, true)
        ));

        log.info("Temporary demo account created - userId={}, expiresAt={}", user.getId(), user.getExpiresAt());
        return new DemoCredentials(user.getId(), email, password);
    }

    @Transactional
    public boolean deleteDemoAccount(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isDemoAccount)
                .map(user -> {
                    deleteAccount(user);
                    tagRepository.deleteAllWithNoNotes();
                    log.info("Temporary demo account deleted - userId={}", userId);
                    return true;
                })
                .orElse(false);
    }

    @Scheduled(fixedDelayString = "${app.demo.cleanup-interval:PT15M}")
    @Transactional
    public void deleteExpiredDemoAccounts() {
        List<User> expiredUsers = userRepository
                .findAllByDemoAccountTrueAndExpiresAtBefore(Instant.now());

        expiredUsers.forEach(this::deleteAccount);
        if (!expiredUsers.isEmpty()) {
            tagRepository.deleteAllWithNoNotes();
            log.info("Expired demo accounts deleted - count={}", expiredUsers.size());
        }
    }

    private void deleteAccount(User user) {
        List<Note> notes = noteRepository.findAllByUserId(user.getId());
        notes.forEach(note -> note.getTags().clear());
        noteRepository.deleteAll(notes);
        noteRepository.flush();

        userRepository.delete(user);
        userRepository.flush();
    }

    private Tag findOrCreateTag(String name) {
        return tagRepository.findByTag(name)
                .orElseGet(() -> tagRepository.save(Tag.builder().tag(name).build()));
    }

    private Note note(
            User user,
            String title,
            String content,
            Instant created,
            List<Tag> tags,
            boolean pinned,
            boolean archived,
            boolean deleted
    ) {
        return Note.builder()
                .user(user)
                .title(title)
                .content(content.strip())
                .created(created)
                .updated(created)
                .tags(tags)
                .pinned(pinned)
                .archived(archived)
                .deleted(deleted)
                .build();
    }

    public record DemoCredentials(Long userId, String email, String password) {
    }
}
