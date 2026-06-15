'use strict';

import { getCsrfToken, getCsrfHeader } from './notes-utils.js';
import { removeNoteCard } from './note-dom.js';

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

    const unarchivedNote = await response.json();
    removeNoteCard(unarchivedNote.id, 'archiveList', 'archiveEmptyState');
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

    const deletedNote = await response.json();
    removeNoteCard(deletedNote.id, 'archiveList', 'archiveEmptyState');
};
