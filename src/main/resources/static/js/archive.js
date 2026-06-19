'use strict';

import { getCsrfToken, getCsrfHeader } from './notes-utils.js';
import { initNoteSelection } from './note-selection.js';

document.addEventListener('DOMContentLoaded', () => {
    initNoteSelection({
        actions: {
            restore: {
                method: 'PATCH',
                url: noteId => `/api/notes/${noteId}/unarchive`,
                confirmMessage: count => `${count} Notiz(en) wiederherstellen?`,
                errorMessage: 'Wiederherstellen der ausgewählten Notizen fehlgeschlagen'
            },
            delete: {
                method: 'PATCH',
                url: noteId => `/api/notes/${noteId}/delete`,
                confirmMessage: count => `${count} Notiz(en) in den Papierkorb verschieben?`,
                errorMessage: 'Verschieben der ausgewählten Notizen fehlgeschlagen'
            }
        }
    });
});

window.unarchiveNote = async function(noteId) {
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();

    const response = await fetch(`/api/notes/${noteId}/unarchive`, {
        method: 'PATCH',
        headers: { [csrfHeader]: csrfToken }
    });

    if (!response.ok) {
        alert('Wiederherstellen fehlgeschlagen');
        return;
    }

    window.location.reload();
};

window.deleteArchivedNote = async function(noteId) {
    if (!confirm('Notiz in den Papierkorb verschieben?')) return;

    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();

    const response = await fetch(`/api/notes/${noteId}/delete`, {
        method: 'PATCH',
        headers: { [csrfHeader]: csrfToken }
    });

    if (!response.ok) {
        alert('Verschieben in den Papierkorb fehlgeschlagen');
        return;
    }

    window.location.reload();
};
