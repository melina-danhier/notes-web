-- Insert test user if not exists
INSERT INTO users (username, email, password, role)
SELECT 'Test User', 'test@notes.com', '$2a$10$ybnvkRZz5.N1rRsxQgDRquuMCXUZ3fAD/6qJ8gB/nE5V0.XJ4S1T2', 'ROLE_USER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'test@notes.com');

-- Insert tags for test notes (skip duplicates gracefully)
INSERT INTO tags (tag) SELECT 'wichtig' WHERE NOT EXISTS (SELECT 1 FROM tags WHERE tag = 'wichtig');
INSERT INTO tags (tag) SELECT 'arbeit' WHERE NOT EXISTS (SELECT 1 FROM tags WHERE tag = 'arbeit');
INSERT INTO tags (tag) SELECT 'persönlich' WHERE NOT EXISTS (SELECT 1 FROM tags WHERE tag = 'persönlich');
INSERT INTO tags (tag) SELECT 'idee' WHERE NOT EXISTS (SELECT 1 FROM tags WHERE tag = 'idee');
INSERT INTO tags (tag) SELECT 'todo' WHERE NOT EXISTS (SELECT 1 FROM tags WHERE tag = 'todo');
INSERT INTO tags (tag) SELECT 'doku' WHERE NOT EXISTS (SELECT 1 FROM tags WHERE tag = 'doku');
INSERT INTO tags (tag) SELECT 'bug' WHERE NOT EXISTS (SELECT 1 FROM tags WHERE tag = 'bug');
INSERT INTO tags (tag) SELECT 'feature' WHERE NOT EXISTS (SELECT 1 FROM tags WHERE tag = 'feature');

-- Insert 10 test notes for the test user
INSERT INTO notes (title, content, created, updated, deleted, user_id)
VALUES
    (
        'Willkommen bei Notes!',
        'Dies ist deine erste Testnotiz. Du kannst Notizen erstellen, bearbeiten und mit Tags organisieren. Nutze die Suche, die Sortierung und die verschiedenen Filter.',
        CURRENT_TIMESTAMP - INTERVAL '10 days',
        CURRENT_TIMESTAMP - INTERVAL '10 days',
        false,
        (SELECT id FROM users WHERE email = 'test@notes.com')
    ),
    (
        'Projektideen sammeln',
        '- Webseite für Hobby-Projekte\n- Mobile App für Notizenverwaltung\n- Blog mit eigenen Erfahrungen\n- Open-Source-Beitrag\n- Discord-Bot-Integration',
        CURRENT_TIMESTAMP - INTERVAL '9 days',
        CURRENT_TIMESTAMP - INTERVAL '8 days',
        false,
        (SELECT id FROM users WHERE email = 'test@notes.com')
    ),
    (
        'Spring Boot Best Practices',
        'Wichtige Punkte zum Merken:\n- Dependency Injection verwenden\n- Configuration Properties nutzen\n- Exception Handling zentralisieren\n- Logging richtig einsetzen\n- Unit Tests für Services\n- Integration Tests mit TestContainers',
        CURRENT_TIMESTAMP - INTERVAL '7 days',
        CURRENT_TIMESTAMP - INTERVAL '7 days',
        false,
        (SELECT id FROM users WHERE email = 'test@notes.com')
    ),
    (
        'API-Design Guidelines',
        'RESTful Prinzipien:\n- Verwendung korrekter HTTP-Methoden (GET, POST, PUT, DELETE)\n- Aussagekräftige URLs (/api/notes /api/users)\n- Angemessene HTTP-Statuscodes (200, 201, 400, 404, 500)\n- Versionierung in der URL oder im Header\n- JSON-Format für Anfragen / Antworten\n- CORS-Konfiguration für Browser-Zugriff',
        CURRENT_TIMESTAMP - INTERVAL '6 days',
        CURRENT_TIMESTAMP - INTERVAL '5 days',
        false,
        (SELECT id FROM users WHERE email = 'test@notes.com')
    ),
    (
        'Datenbank-Skripts',
        'PostgreSQL Anfragen zum Testen:\n\nAlle Benutzer auswählen:\nSELECT * FROM users;\n\nNotizen mit Tags abrufen:\nSELECT n.title, n.content, t.tag FROM notes n\nJOIN note_tag nt ON n.id = nt.note_id\nJOIN tags t ON nt.tag_id = t.id;',
        CURRENT_TIMESTAMP - INTERVAL '5 days',
        CURRENT_TIMESTAMP - INTERVAL '4 days',
        false,
        (SELECT id FROM users WHERE email = 'test@notes.com')
    ),
    (
        'Frontend-Features zu testen',
        '✓ Notiz-Editor mit Live-Vorschau\n✓ Tag-Eingabe beim Erstellen/Bearbeiten\n✓ Suche nach Titel und Inhalt\n✓ Sortierung nach Erstellungs-/Änderungsdatum\n✓ Soft-Delete mit Papierkorb\n✓ Pagination bei vielen Notizen\n✓ Mobile-responsive Design\n✓ Dunkler Modus (falls implementiert)',
        CURRENT_TIMESTAMP - INTERVAL '4 days',
        CURRENT_TIMESTAMP - INTERVAL '3 days',
        false,
        (SELECT id FROM users WHERE email = 'test@notes.com')
    ),
    (
        'Security-Checkliste',
        'Sicherheitsmaßnahmen überprüfen:\n- CSRF-Token auf allen Formularen\n- Input-Validierung auf Client und Server\n- SQL-Injection-Schutz (parameterisierte Queries)\n- Password Encoding mit BCrypt\n- Session-Management\n- HTTPS in Production\n- CORS eingeschränkt auf trusted Origins\n- Spring Security konfiguriert',
        CURRENT_TIMESTAMP - INTERVAL '3 days',
        CURRENT_TIMESTAMP - INTERVAL '2 days',
        false,
        (SELECT id FROM users WHERE email = 'test@notes.com')
    ),
    (
        'Docker & Deployment vorbereiten',
        'Docker-Setup überprüfen:\n- Dockerfile für Java 21\n- Docker-Compose mit PostgreSQL\n- Environment-Variablen für Prod und Dev\n- Health-Check Endpoints\n- Logs zum stdout leiten\n- Non-root User im Container\n- Render.yml für Cloud-Deployment erstellen',
        CURRENT_TIMESTAMP - INTERVAL '2 days',
        CURRENT_TIMESTAMP - INTERVAL '1 days',
        false,
        (SELECT id FROM users WHERE email = 'test@notes.com')
    ),
    (
        'Screenshots und Dokumentation',
        'Für Portfolio vorbereiten:\n1. Dashboard-Screenshot (10 Notizen sichtbar)\n2. Note-Editor mit Tags\n3. Filter/Suchfunktion Demo\n4. Papierkorb-View\n5. Kurze Demo-GIF\n\nREADME aktualisieren mit:\n- Feature-Übersicht\n- Tech-Stack\n- Deployment-Anleitung\n- Test-Kredenziale',
        CURRENT_TIMESTAMP - INTERVAL '1 days',
        CURRENT_TIMESTAMP - INTERVAL '1 hours',
        false,
        (SELECT id FROM users WHERE email = 'test@notes.com')
    ),
    (
        'Nächste Schritte & Ideen',
        'Mögliche Erweiterungen:\n- Markdown-Rendering\n- Notiz-Versionshistorie\n- Zusammenarbeit / Notiz-Sharing\n- Erinnerungen und Deadlines\n- Export zu PDF/Word\n- Browser-Offline-Unterstützung (PWA)\n- Sync zwischen Browser und Mobile\n- Notiz-Templates\n\nZuerst: Testing und Dokumentation abschließen',
        CURRENT_TIMESTAMP - INTERVAL '6 hours',
        CURRENT_TIMESTAMP - INTERVAL '2 hours',
        false,
        (SELECT id FROM users WHERE email = 'test@notes.com')
    );

-- Insert one deleted note for trash workflow test
INSERT INTO notes (title, content, created, updated, deleted, user_id)
VALUES
    (
        'Alte, gelöschte Notiz',
        'Diese Notiz wurde gelöscht und sollte im Papierkorb angezeigt werden. Sie kann von dort wiederhergestellt oder permanent gelöscht werden.',
        CURRENT_TIMESTAMP - INTERVAL '30 days',
        CURRENT_TIMESTAMP - INTERVAL '1 days',
        true,
        (SELECT id FROM users WHERE email = 'test@notes.com')
    );

-- Associate tags with notes
-- Note 1: "Willkommen" - wichtig, persönlich
INSERT INTO note_tag (note_id, tag_id)
SELECT n.id, t.id FROM notes n, tags t
WHERE n.title = 'Willkommen bei Notes!' AND t.tag IN ('wichtig', 'persönlich')
AND NOT EXISTS (SELECT 1 FROM note_tag WHERE note_id = n.id AND tag_id = t.id);

-- Note 2: "Projektideen" - idee, todo
INSERT INTO note_tag (note_id, tag_id)
SELECT n.id, t.id FROM notes n, tags t
WHERE n.title = 'Projektideen sammeln' AND t.tag IN ('idee', 'todo')
AND NOT EXISTS (SELECT 1 FROM note_tag WHERE note_id = n.id AND tag_id = t.id);

-- Note 3: "Spring Boot" - arbeit, doku
INSERT INTO note_tag (note_id, tag_id)
SELECT n.id, t.id FROM notes n, tags t
WHERE n.title = 'Spring Boot Best Practices' AND t.tag IN ('arbeit', 'doku')
AND NOT EXISTS (SELECT 1 FROM note_tag WHERE note_id = n.id AND tag_id = t.id);

-- Note 4: "API-Design" - arbeit, doku
INSERT INTO note_tag (note_id, tag_id)
SELECT n.id, t.id FROM notes n, tags t
WHERE n.title = 'API-Design Guidelines' AND t.tag IN ('arbeit', 'doku')
AND NOT EXISTS (SELECT 1 FROM note_tag WHERE note_id = n.id AND tag_id = t.id);

-- Note 5: "Datenbank-Skripts" - arbeit
INSERT INTO note_tag (note_id, tag_id)
SELECT n.id, t.id FROM notes n, tags t
WHERE n.title = 'Datenbank-Skripts' AND t.tag = 'arbeit'
AND NOT EXISTS (SELECT 1 FROM note_tag WHERE note_id = n.id AND tag_id = t.id);

-- Note 6: "Frontend-Features" - todo, feature
INSERT INTO note_tag (note_id, tag_id)
SELECT n.id, t.id FROM notes n, tags t
WHERE n.title = 'Frontend-Features zu testen' AND t.tag IN ('todo', 'feature')
AND NOT EXISTS (SELECT 1 FROM note_tag WHERE note_id = n.id AND tag_id = t.id);

-- Note 7: "Security-Checkliste" - bug, wichtig
INSERT INTO note_tag (note_id, tag_id)
SELECT n.id, t.id FROM notes n, tags t
WHERE n.title = 'Security-Checkliste' AND t.tag IN ('bug', 'wichtig')
AND NOT EXISTS (SELECT 1 FROM note_tag WHERE note_id = n.id AND tag_id = t.id);

-- Note 8: "Docker & Deployment" - arbeit, todo
INSERT INTO note_tag (note_id, tag_id)
SELECT n.id, t.id FROM notes n, tags t
WHERE n.title = 'Docker & Deployment vorbereiten' AND t.tag IN ('arbeit', 'todo')
AND NOT EXISTS (SELECT 1 FROM note_tag WHERE note_id = n.id AND tag_id = t.id);

-- Note 9: "Screenshots" - todo, doku
INSERT INTO note_tag (note_id, tag_id)
SELECT n.id, t.id FROM notes n, tags t
WHERE n.title = 'Screenshots und Dokumentation' AND t.tag IN ('todo', 'doku')
AND NOT EXISTS (SELECT 1 FROM note_tag WHERE note_id = n.id AND tag_id = t.id);

-- Note 10: "Nächste Schritte" - idee, feature
INSERT INTO note_tag (note_id, tag_id)
SELECT n.id, t.id FROM notes n, tags t
WHERE n.title = 'Nächste Schritte & Ideen' AND t.tag IN ('idee', 'feature', 'todo')
AND NOT EXISTS (SELECT 1 FROM note_tag WHERE note_id = n.id AND tag_id = t.id);


