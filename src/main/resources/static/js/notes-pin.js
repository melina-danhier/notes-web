'use strict';

import { getCsrfHeader, getCsrfToken } from './notes-utils.js';

export function togglePin(noteId, isFromModal = false) {
    const btn = document.querySelector(`#note-${noteId} .pin-btn`);
    if (!btn) return;

    // Disable button during request
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
        return response.json();
    })
    .then(data => {
        // Normal view: reload immediately (no visual feedback)
        if (!isFromModal) {
            window.location.reload();
            return;
        }

        // Modal view: only update modal button, not background button
        const newPinnedState = data.pinned;
        const modalPinBtn = document.getElementById('modalPinBtn');
        if (modalPinBtn) {
            updatePinButton(modalPinBtn, newPinnedState);
        }
    })
    .catch(error => {
        console.error('Fehler beim Anheften der Notiz:', error);
        alert('Fehler beim Anheften der Notiz');
        btn.disabled = false;
    });
}

function updatePinButton(btn, isPinned) {
    btn.innerHTML = isPinned ? '📍 Entfernen' : '📌 Anheften';
    btn.title = isPinned ? 'Trennen' : 'Anheften';
    btn.dataset.pinned = isPinned;
    btn.disabled = false;

    // Update button color
    btn.className = isPinned
        ? 'btn btn-warning px-3 py-1 pin-btn'
        : 'btn btn-secondary px-3 py-1 pin-btn';
}

// Export for global use
window.togglePin = togglePin;


