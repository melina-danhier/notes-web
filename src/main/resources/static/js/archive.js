'use strict';

import { getCsrfToken, getCsrfHeader } from './notes-utils.js';

window.unarchiveNote = async function(noteId) {
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();

    const response = await fetch(`/api/notes/${noteId}/unarchive`, {
        method: 'PATCH',
        headers: { [csrfHeader]: csrfToken }
    });

    if (response.ok) {
        location.reload();
    } else {
        alert('Wiederherstellen fehlgeschlagen');
    }
};

window.deleteArchivedNote = async function(noteId) {
    if (!confirm('Notiz in den Papierkorb verschieben?')) return;

    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();

    const response = await fetch(`/api/notes/${noteId}/delete`, {
        method: 'PATCH',
        headers: { [csrfHeader]: csrfToken }
    });

    if (response.ok) {
        location.reload();
    } else {
        alert('Verschieben in den Papierkorb fehlgeschlagen');
    }
};
