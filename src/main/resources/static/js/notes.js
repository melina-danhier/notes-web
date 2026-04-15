'use strict';

import { initNotesApp } from './notes-init.js';
import './notes-delete.js';
import { initSorting } from './note-sort.js';

document.addEventListener('DOMContentLoaded', () => {
    initNotesApp()
    initSorting();
    initSearchAndTagFilter();
});

/**
 * Suche & Tag-Filter – leitet an das Backend weiter (URL-Parameter)
 */
function initSearchAndTagFilter() {
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        // Enter → Backend-Request
        searchInput.addEventListener('keypress', e => {
            if (e.key === 'Enter') {
                e.preventDefault();
                navigateWithSearch(searchInput.value.trim());
            }
        });
    }

    // Tag-Filter Dropdown
    document.querySelectorAll('.tag-filter-item').forEach(item => {
        item.onclick = e => {
            e.preventDefault();
            e.stopPropagation();
            const tagName = item.dataset.tagName;
            const tagValue = item.dataset.tagValue || '';
            navigateWithTag(tagValue, tagName);
        };
    });
}

function navigateWithSearch(query) {
    const url = new URL(window.location.href);
    if (query) {
        url.searchParams.set('search', query);
    } else {
        url.searchParams.delete('search');
    }
    url.searchParams.set('pageNo', '0');
    window.location.href = url.toString();
}

function navigateWithTag(tag, displayName) {
    const url = new URL(window.location.href);
    if (tag) {
        url.searchParams.set('tagFilter', tag);
    } else {
        url.searchParams.delete('tagFilter');
    }
    url.searchParams.set('pageNo', '0');
    window.location.href = url.toString();
}