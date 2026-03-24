'use strict';

const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

// Neue Notiz
document.getElementById('newNoteBtn').addEventListener('click', () => {
    window.location.href = '/notes/new';
});

// Löschen
// ✅ Bereits korrekt, aber CSRF-Header fixen:
window.deleteNote = function(noteId) {
    if (!confirm('Notiz wirklich löschen?')) return;

    fetch(`/api/notes/${noteId}`, {
        method: 'DELETE',
        headers: {
            [csrfHeader]: csrfToken  // ← csrfHeader Variable nutzen
        }
    })
        .then(response => {
            if (response.ok) {
                document.getElementById(`note-${noteId}`).style.transition = 'opacity 0.3s';
                document.getElementById(`note-${noteId}`).style.opacity = '0';
                setTimeout(() => document.getElementById(`note-${noteId}`).remove(), 300);
            }
        })
        .catch(error => alert('Fehler beim Löschen: ' + error));
};

// Logout
function logout(event) {
    event.preventDefault();
    fetch('/logout', {
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
        }
    }).then(() => location.reload());
}