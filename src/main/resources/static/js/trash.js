'use strict';

import { getCsrfToken, getCsrfHeader } from './notes-utils.js';

document.addEventListener('DOMContentLoaded', () => {
    initEmptyTrashButton();
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

    if (response.ok) {
        location.reload();
    } else {
        alert('Wiederherstellen fehlgeschlagen');
    }
};

window.forceDeleteNote = async function(noteId) {
    if (!confirm('Notiz endgültig löschen?')) return;

    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();

    const response = await fetch(`/api/notes/${noteId}`, {
        method: 'DELETE',
        headers: { [csrfHeader]: csrfToken }
    });

    if (response.ok) {
        location.reload();
    } else {
        alert('Endgültiges Löschen fehlgeschlagen');
    }
};

async function emptyTrash() {
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();

    const response = await fetch('/api/notes/trash/empty', {
        method: 'DELETE',
        headers: { [csrfHeader]: csrfToken }
    });

    if (response.ok) {
        location.reload();
    } else {
        alert('Papierkorb konnte nicht geleert werden');
    }
}