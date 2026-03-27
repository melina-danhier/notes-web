'use strict';

import { initNotesApp } from './notes-init.js';
import './notes-delete.js';
import { initSearchAndFilter } from './notes-filter.js';

document.addEventListener('DOMContentLoaded', function() {
    initNotesApp();
    initSearchAndFilter();

    // Event Listener
    document.getElementById('clearSearchBtn')?.addEventListener('click', window.clearSearch);

    // "Alle Tags" initial aktiv
    const allTagsBtn = document.querySelector('.tag-filter-item[data-tag-id=""]');
    if (allTagsBtn) {
        allTagsBtn.classList.add('active-tag-filter');
    }
});