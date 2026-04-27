'use strict';

import { initNotesApp } from './notes-init.js';
import './notes-delete.js';
import { initSorting } from './note-sort.js';

document.addEventListener('DOMContentLoaded', () => {
    initNotesApp()
    initSorting();
    initSearchAndTagFilter();
    initBinButton();
});

function initBinButton() {
    const binButton = document.getElementById('binButton');
    if (!binButton) return;

    binButton.addEventListener('click', (e) => {
        e.preventDefault();
        window.location.href = '/notes/trash';
    });
}

function initSearchAndTagFilter() {
    const searchInput = document.getElementById('searchInput');
    const clearBtn = document.getElementById('clearSearchBtn');

    if (searchInput) {
        searchInput.addEventListener('keydown', e => {
            if (e.key === 'Enter') {
                e.preventDefault();
                navigateWithSearch(searchInput.value.trim());
            }
        });
    }

    if (clearBtn && searchInput) {
        clearBtn.addEventListener('click', (e) => {
            e.preventDefault();

            const query = searchInput.value.trim();
            if (!query) {
                return;
            }

            searchInput.value = '';
            navigateWithSearch('');
        });
    }

    document.querySelectorAll('.tag-filter-item').forEach(item => {
        item.onclick = e => {
            e.preventDefault();
            e.stopPropagation();
            const tagValue = item.dataset.tagValue || '';
            navigateWithTag(tagValue);
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

function navigateWithTag(tag) {
    const url = new URL(window.location.href);
    if (tag) {
        url.searchParams.set('tagFilter', tag);
    } else {
        url.searchParams.delete('tagFilter');
    }
    url.searchParams.set('pageNo', '0');
    window.location.href = url.toString();
}