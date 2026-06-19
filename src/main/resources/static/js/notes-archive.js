'use strict';

import { getCsrfToken, getCsrfHeader, showLoading, hideLoading } from './notes-utils.js';

window.archiveNote = async function(noteId) {
    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();

    if (!csrfToken || !csrfHeader) {
        alert('Sicherheitstoken fehlt!');
        return;
    }

    showLoading(noteId);

    try {
        const response = await fetch(`/api/notes/${noteId}/archive`, {
            method: 'PATCH',
            headers: { [csrfHeader]: csrfToken }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        window.location.reload();
    } catch (error) {
        console.error('Archivierungsfehler:', error);
        hideLoading(noteId);
        alert('Fehler: ' + error.message);
    }
};
