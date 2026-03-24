'use strict';

const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
const saveBtn = document.getElementById('saveBtn');

// 🆕 SAVE: POST /api/notes (Create) ODER PUT /api/notes/{id} (Update)
saveBtn.addEventListener('click', function(e) {
    e.preventDefault();

    const title = document.getElementById('titleInput').value.trim();
    if (!title) {
        alert('Titel ist erforderlich!');
        return;
    }

    // ✅ EditNoteDTO-Struktur (genau wie Backend erwartet)
    const noteData = {
        id: document.getElementById('noteIdInput').value || null,
        title: title,
        content: document.getElementById('contentInput').value || '',
        tagsRaw: document.getElementById('tagsInput').value || ''
        // tags: [] wird im Backend verarbeitet
    };

    const noteId = noteData.id;
    const url = noteId ? `/api/notes/${noteId}` : '/api/notes';
    const method = noteId ? 'PUT' : 'POST';

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify(noteData)
    })
        .then(response => {
            if (response.ok) {
                showSuccess('✅ Notiz gespeichert!');
                setTimeout(() => window.location.href = '/notes', 1000);
            } else {
                return response.text().then(text => {
                    throw new Error(`HTTP ${response.status}: ${text}`);
                });
            }
        })
        .catch(error => showError(`❌ Fehler: ${error.message}`));
});

// 🆕 DELETE: Korrigiert (noteId aus Input)
window.deleteNote = function() {
    const noteId = document.getElementById('noteIdInput').value;
    if (!noteId || !confirm('Notiz wirklich löschen?')) return;

    fetch(`/api/notes/${noteId}`, {
        method: 'DELETE',
        headers: { [csrfHeader]: csrfToken }
    })
        .then(response => {
            if (response.ok) {
                showSuccess('✅ Notiz gelöscht!');
                setTimeout(() => window.location.href = '/notes', 1000);
            }
        })
        .catch(error => showError('❌ Fehler beim Löschen'));
};

// Rest unverändert...
document.addEventListener('DOMContentLoaded', function() {
    const titleInput = document.getElementById('titleInput');
    titleInput.focus();

    const textarea = document.getElementById('contentInput');
    textarea.addEventListener('input', function() {
        this.style.height = 'auto';
        this.style.height = Math.min(this.scrollHeight, 300) + 'px';
    });
    textarea.dispatchEvent(new Event('input'));

    document.addEventListener('keydown', function(e) {
        if ((e.ctrlKey || e.metaKey) && e.key === 's') {
            e.preventDefault();
            saveBtn.click();
        }
    });
});

function showSuccess(msg) { showNotification(msg, 'success'); }
function showError(msg) { showNotification(msg, 'error'); }
function showNotification(msg, type) {
    const div = document.createElement('div');
    div.className = `notification ${type}`;
    div.textContent = msg;
    document.body.appendChild(div);
    setTimeout(() => div.remove(), 2500);
}