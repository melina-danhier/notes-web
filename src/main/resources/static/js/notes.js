'use strict';

/**
 * Notes App JavaScript
 * Alle Event Listener und API Calls
 */

// Warte bis DOM vollständig geladen ist
document.addEventListener('DOMContentLoaded', function() {
    initNotesApp();
});

function initNotesApp() {
    // CSRF Token & Header
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();

    if (!csrfToken || !csrfHeader) {
        console.error('CSRF Token nicht gefunden!');
        return;
    }

    initNewNoteButton();
    initLogoutForm();
    initSearch();
    updateLocalTimes();
    initTagFilter();

}

/**
 * Neue Notiz Button initialisieren
 */
function initNewNoteButton() {
    const newNoteBtn = document.getElementById('newNoteBtn');
    if (newNoteBtn) {
        newNoteBtn.addEventListener('click', function(e) {
            e.preventDefault();
            window.location.href = '/notes/new';
        });
    }
}

/**
 * Logout Form initialisieren
 */
function initLogoutForm() {
    const logoutForm = document.querySelector('form[action="/logout"]');
    if (logoutForm) {
        logoutForm.addEventListener('submit', function(e) {
            if (!confirm('Wirklich abmelden?')) {
                e.preventDefault();
                return false;
            }
        });
    }
}

/**
 * Suche/Filter initialisieren (optional)
 */
function initSearch() {
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            filterNotes(this.value);
        });
    }
}

/**
 * Notiz filtern/suchen (Client-seitig)
 */
function filterNotes(searchTerm) {
    const notes = document.querySelectorAll('[id^="note-"]');
    const term = searchTerm.toLowerCase();

    notes.forEach(note => {
        const title = note.querySelector('h4')?.textContent.toLowerCase() || '';
        const content = note.querySelector('.note-content')?.textContent.toLowerCase() || '';
        const tags = note.querySelectorAll('[class*="bg-primary"]');
        let hasTagMatch = false;

        tags.forEach(tag => {
            if (tag.textContent.toLowerCase().includes(term)) {
                hasTagMatch = true;
            }
        });

        if (title.includes(term) || content.includes(term) || hasTagMatch) {
            note.style.display = '';
        } else {
            note.style.display = 'none';
        }
    });
}

/**
 * CSRF Token abrufen
 */
function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
}

/**
 * CSRF Header Name abrufen
 */
function getCsrfHeader() {
    return document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
}

// ═══════════════════════════════════════════════════════════════════════════════
// GLOBAL FUNCTIONS (für Thymeleaf onclick)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Notiz löschen (Globale Funktion für Thymeleaf)
 */
window.deleteNote = function(noteId) {
    if (!confirm('Notiz wirklich löschen?\n\nDiese Aktion kann nicht rückgängig gemacht werden!')) {
        return;
    }

    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();

    if (!csrfToken || !csrfHeader) {
        alert('Sicherheitstoken fehlt. Bitte Seite neu laden.');
        return;
    }

    showLoading(noteId);

    fetch(`/api/notes/${noteId}`, {
        method: 'DELETE',
        headers: {
            [csrfHeader]: csrfToken,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (response.ok) {
                removeNoteWithAnimation(noteId);
            } else {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
        })
        .catch(error => {
            console.error('Löschfehler:', error);
            hideLoading(noteId);
            alert(`Fehler beim Löschen: ${error.message}`);
        });
};

/**
 * Loading Animation zeigen
 */
function showLoading(noteId) {
    const noteElement = document.getElementById(`note-${noteId}`);
    if (noteElement) {
        const deleteBtn = noteElement.querySelector('button[title="Löschen"]');
        if (deleteBtn) {
            deleteBtn.innerHTML = '⏳';
            deleteBtn.disabled = true;
        }
    }
}

/**
 * Lokale Zeit formatieren (deutsches Format)
 */
function formatLocalTime(isoString) {
    const options = {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    };
    return new Date(isoString).toLocaleDateString('de-DE', options);
}

/**
 * Server-Zeit (UTC) -> Lokale Zeit konvertieren
 */
function updateLocalTimes() {
    document.querySelectorAll('.local-time span').forEach(timeSpan => {
        const serverText = timeSpan.textContent.trim();

        // Prüfe ob es eine gültige Server-Zeit ist: dd.MM.yyyy HH:mm
        if (!serverText.match(/^\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}$/)) return;

        // Server-Zeit parsen: "25.03.2026 14:36" -> Date Objekt (als UTC interpretieren)
        const [datePart, timePart] = serverText.split(' ');
        const [day, month, year] = datePart.split('.').map(Number);
        const [hour, minute] = timePart.split(':').map(Number);

        // UTC Date erstellen (Server-Zeit = UTC)
        const utcDate = new Date(Date.UTC(year, month - 1, day, hour, minute));

        // In lokale Zeit umwandeln
        const localTime = utcDate.toLocaleDateString('de-DE', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        });

        // Ersetzen
        timeSpan.textContent = localTime;
    });
}

/**
 * Loading Animation verstecken
 */
function hideLoading(noteId) {
    const noteElement = document.getElementById(`note-${noteId}`);
    if (noteElement) {
        const deleteBtn = noteElement.querySelector('button[title="Löschen"]');
        if (deleteBtn) {
            deleteBtn.innerHTML = '🗑️';
            deleteBtn.disabled = false;
        }
    }
}

/**
 * Notiz mit Animation entfernen
 */
function removeNoteWithAnimation(noteId) {
    const noteElement = document.getElementById(`note-${noteId}`);
    if (noteElement) {
        noteElement.style.transition = 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)';
        noteElement.style.opacity = '0';
        noteElement.style.transform = 'translateX(-20px) scale(0.95)';
        noteElement.style.maxHeight = noteElement.scrollHeight + 'px';

        setTimeout(() => {
            noteElement.style.maxHeight = '0';
            noteElement.style.margin = '0';
            noteElement.style.padding = '0';
        }, 100);

        setTimeout(() => {
            noteElement.remove();
            checkEmptyState();
        }, 400);
    }
}

/**
 * Prüfen ob noch Notizen vorhanden sind
 */
function checkEmptyState() {
    const notesList = document.getElementById('notesList');
    const notes = document.querySelectorAll('[id^="note-"]');

    if (notes.length === 0 && notesList) {
        notesList.innerHTML = `
            <div class="col-12">
                <div class="text-center py-5">
                    <h5 class="fst-italic text-muted">Keine Notizen vorhanden</h5>
                    <p class="text-muted">Erstelle deine erste Notiz mit dem ➕ Button!</p>
                </div>
            </div>
        `;
    }
}

/**
 * Tag Filter Dropdown initialisieren
 */
function initTagFilter() {
    const tagFilterItems = document.querySelectorAll('.tag-filter-item');

    tagFilterItems.forEach(item => {
        item.addEventListener('click', function(e) {
            e.preventDefault();

            // Active Klasse setzen
            document.querySelectorAll('.tag-filter-item').forEach(i => i.classList.remove('active-tag-filter'));
            this.classList.add('active-tag-filter');

            // Button Text aktualisieren
            const tagName = this.getAttribute('data-tag-name');
            const tagId = this.getAttribute('data-tag-id');
            document.getElementById('selectedTagDisplay').textContent = tagName;

            // Filter anwenden
            filterNotesByTag(tagId);

            // Dropdown schließen
            const dropdown = document.getElementById('tagFilterBtn');
            const bsDropdown = new bootstrap.Dropdown(dropdown);
            bsDropdown.hide();
        });
    });
}

/**
 * Notizen nach Tag filtern (ROBUSTE VERSION)
 */
function filterNotesByTag(tagId) {
    const notes = document.querySelectorAll('[id^="note-"]');

    notes.forEach(note => {
        const tags = note.querySelectorAll('span[class*="bg-primary"]');
        let hasMatchingTag = false;

        // "Alle Tags" - alle anzeigen
        if (tagId === '' || tagId === null || tagId === 'null' || tagId === undefined) {
            hasMatchingTag = true;
        } else {
            // ZWEIFACHE PRÜFUNG für maximale Kompatibilität:
            tags.forEach(tagSpan => {
                // 1. Prüfung: data-tag-id Attribut (primär)
                if (tagSpan.dataset.tagId === tagId) {
                    hasMatchingTag = true;
                    return; // Sofortiges Match gefunden
                }
                // 2. Fallback: Text-Vergleich (für alte Notizen ohne data-tag-id)
                const tagText = tagSpan.textContent.trim();
                const filterTagName = document.querySelector(`[data-tag-id="${tagId}"]`)?.getAttribute('data-tag-name') || '';
                if (tagText === filterTagName) {
                    hasMatchingTag = true;
                }
            });
        }

        if (hasMatchingTag) {
            note.style.display = '';
            note.classList.remove('filtered-out');
        } else {
            note.style.display = 'none';
            note.classList.add('filtered-out');
        }
    });
}
