'use strict';
import { updateLocalTimes } from './notes-utils.js';
import { initSearch, initTagFilter } from './notes-filter.js';

/**
 * APP INITIALISIERUNG & EVENTS
 */
export function initNotesApp() {
    initNewNoteButton();
    initLogoutForm();
    initSearch();
    initTagFilter();
    updateLocalTimes();
}

function initNewNoteButton() {
    const btn = document.getElementById('newNoteBtn');
    if (btn) btn.onclick = (e) => {
        e.preventDefault();
        window.location.href = '/notes/new';
    };
}

function initLogoutForm() {
    const form = document.querySelector('form[action="/logout"]');
    if (form) form.onsubmit = (e) => {
        if (!confirm('Wirklich abmelden?')) e.preventDefault();
    };
}