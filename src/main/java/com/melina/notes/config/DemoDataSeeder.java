package com.melina.notes.config;

import com.melina.notes.entity.Note;
import com.melina.notes.entity.Tag;
import com.melina.notes.entity.User;
import com.melina.notes.repository.NoteRepository;
import com.melina.notes.repository.TagRepository;
import com.melina.notes.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final TagRepository tagRepository;
    private final PasswordEncoder passwordEncoder;
    private final String displayName;
    private final String email;
    private final String password;

    public DemoDataSeeder(
            UserRepository userRepository,
            NoteRepository noteRepository,
            TagRepository tagRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.demo-data.user.display-name}") String displayName,
            @Value("${app.demo-data.user.email}") String email,
            @Value("${app.demo-data.user.password:}") String password
    ) {
        this.userRepository = userRepository;
        this.noteRepository = noteRepository;
        this.tagRepository = tagRepository;
        this.passwordEncoder = passwordEncoder;
        this.displayName = displayName;
        this.email = email.trim().toLowerCase();
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Demo data is enabled, but DEMO_USER_PASSWORD is missing or blank"
            );
        }

        if (userRepository.existsByEmail(email)) {
            log.info("Skipping demo data: user already exists - email={}", email);
            return;
        }

        User user = userRepository.save(User.builder()
                .displayName(displayName.trim())
                .email(email)
                .password(passwordEncoder.encode(password))
                .role("ROLE_USER")
                .build());

        Tag start = findOrCreateTag("start");
        Tag markdown = findOrCreateTag("markdown");
        Tag privat = findOrCreateTag("privat");
        Tag ideen = findOrCreateTag("ideen");

        Instant now = Instant.now();
        noteRepository.saveAll(List.of(
                note(user, "Willkommen bei Notes 👋", """
                        # Deine Demo ist bereit

                        Diese Notiz zeigt dir die wichtigsten Funktionen:

                        - Notizen mit **Markdown** formatieren
                        - Tags vergeben
                        - wichtige Notizen anheften
                        - Inhalte suchen, archivieren oder löschen

                        Viel Spaß beim Ausprobieren!
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

        log.info("Demo data created - userId={}, email={}, notes=5", user.getId(), email);
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
}
