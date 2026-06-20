'use strict';
import { updateLocalTimes } from './notes-utils.js';

export function initNotesApp() {
    initNewNoteButton();
    initLogoutForm();
    initPreviewToggle();
    updateLocalTimes();
}

const NOTE_PREVIEW_STORAGE_KEY = 'noteCardPreviewsVisible';

function initNewNoteButton() {
    const btn = document.getElementById('newNoteBtn');
    if (btn) {
        btn.onclick = (e) => {
            e.preventDefault();
            window.location.href = '/notes/new';
        };
    }
}

function initLogoutForm() {
    const form = document.querySelector('form[action="/logout"]');
    if (form) {
        form.onsubmit = (e) => {
            if (!confirm('Wirklich abmelden?')) {
                e.preventDefault();
            }
        };
    }
}

function initPreviewToggle() {
    const button = document.getElementById('previewToggleBtn');
    if (!button) return;

    let previewsVisible = readPreviewPreference();
    updatePreviewVisibility(button, previewsVisible);

    button.addEventListener('click', () => {
        previewsVisible = !previewsVisible;
        updatePreviewVisibility(button, previewsVisible);
        savePreviewPreference(previewsVisible);
    });
}

function updatePreviewVisibility(button, previewsVisible) {
    document.body.classList.toggle('note-previews-hidden', !previewsVisible);
    button.setAttribute('aria-pressed', String(!previewsVisible));
    button.title = previewsVisible
        ? 'Mini-Vorschau der Notizen ausblenden'
        : 'Mini-Vorschau der Notizen einblenden';

    const label = button.querySelector('#previewToggleLabel');
    if (label) {
        label.textContent = previewsVisible ? 'Vorschau aus' : 'Vorschau an';
    }
}

function readPreviewPreference() {
    try {
        return localStorage.getItem(NOTE_PREVIEW_STORAGE_KEY) !== 'false';
    } catch {
        return true;
    }
}

function savePreviewPreference(previewsVisible) {
    try {
        localStorage.setItem(NOTE_PREVIEW_STORAGE_KEY, String(previewsVisible));
    } catch {
        // Die Umschaltung funktioniert auch, wenn der Browser Speicher blockiert.
    }
}
