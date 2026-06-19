'use strict';

import { getCsrfToken, getCsrfHeader } from './notes-utils.js';
import { clearNoteList, removeNoteCard } from './note-dom.js';
import { initNoteSelection } from './note-selection.js';

document.addEventListener('DOMContentLoaded', () => {
    initEmptyTrashButton();
    initNoteSelection({
        actions: {
            restore: {
                method: 'PATCH',
                url: noteId => `/api/notes/${noteId}/restore`,
                confirmMessage: count => `${count} Notiz(en) wiederherstellen?`,
                errorMessage: 'Wiederherstellen der ausgewählten Notizen fehlgeschlagen'
            },
            delete: {
                method: 'DELETE',
                url: noteId => `/api/notes/${noteId}`,
                confirmMessage: count => `${count} Notiz(en) endgültig löschen?`,
                errorMessage: 'Endgültiges Löschen der ausgewählten Notizen fehlgeschlagen'
            }
        }
    });
});

function initEmptyTrashButton() {
    const emptyTrashBtn = document.getElementById('emptyTrashBtn');
    if (!emptyTrashBtn) return;

    emptyTrashBtn.addEventListener('click', async () => {
        if (!confirm('Papierkorb wirklich komplett leeren?')) return;
        await emptyTrash();
    });
}

window.restoreNote = async function(noteId) {
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();

    const response = await fetch(`/api/notes/${noteId}/restore`, {
        method: 'PATCH',
        headers: { [csrfHeader]: csrfToken }
    });

    if (!response.ok) {
        alert('Wiederherstellen fehlgeschlagen');
        return;
    }

    const restoredNote = await response.json();
    removeNoteCard(restoredNote.id, 'trashList', 'trashEmptyState');
};

window.forceDeleteNote = async function(noteId) {
    if (!confirm('Notiz endgültig löschen?')) return;

    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();

    const response = await fetch(`/api/notes/${noteId}`, {
        method: 'DELETE',
        headers: { [csrfHeader]: csrfToken }
    });

    if (!response.ok) {
        alert('Endgültiges Löschen fehlgeschlagen');
        return;
    }

    removeNoteCard(noteId, 'trashList', 'trashEmptyState');
};

async function emptyTrash() {
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();

    const response = await fetch('/api/notes/trash/empty', {
        method: 'DELETE',
        headers: { [csrfHeader]: csrfToken }
    });

    if (!response.ok) {
        alert('Papierkorb konnte nicht geleert werden');
        return;
    }

    clearNoteList('trashList', 'trashEmptyState');
}
