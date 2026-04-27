'use strict';
import { updateLocalTimes } from './notes-utils.js';

export function initNotesApp() {
    initNewNoteButton();
    initLogoutForm();
    updateLocalTimes();
}

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