'use strict';

import { getCsrfHeader, getCsrfToken } from './notes-utils.js';

export function togglePin(noteId) {
    const btn = document.querySelector(`#note-${noteId} .pin-btn`);
    if (!btn) return;

    btn.disabled = true;

    fetch(`/api/notes/${noteId}/pin`, {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json',
            [getCsrfHeader()]: getCsrfToken()
        },
        body: JSON.stringify({})
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        window.location.reload();
    })
    .catch(error => {
        console.error('Fehler beim Anheften der Notiz:', error);
        alert('Fehler beim Anheften der Notiz');
        btn.disabled = false;
    });
}

window.togglePin = togglePin;
