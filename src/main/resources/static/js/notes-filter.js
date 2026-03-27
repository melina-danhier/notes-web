'use strict';

export function initSearchAndFilter() {
    const searchInput = document.getElementById('searchInput');
    if (!searchInput) return;

    // Enter für Suche
    searchInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            performCombinedSearch();
        }
    });

    // Automatische Suche bei Input
    let searchTimeout;
    searchInput.addEventListener('input', function() {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(performCombinedSearch, 300);
    });

    // Tag Filter
    document.querySelectorAll('.tag-filter-item').forEach(item => {
        item.onclick = (e) => {
            e.preventDefault();
            e.stopPropagation();

            document.querySelectorAll('.tag-filter-item').forEach(i => i.classList.remove('active-tag-filter'));
            item.classList.add('active-tag-filter');
            document.getElementById('selectedTagDisplay').textContent = item.dataset.tagName;
            performCombinedSearch();
        };
    });
}

function performCombinedSearch() {
    const query = document.getElementById('searchInput')?.value?.toLowerCase().trim() || '';
    const activeTagFilter = document.querySelector('.tag-filter-item.active-tag-filter');
    const tagId = activeTagFilter ? activeTagFilter.dataset.tagId : null;

    if (!query && !tagId) {
        showAllNotes();
        return;
    }

    const notes = document.querySelectorAll('#notesList > div');
    let hasResults = false;

    notes.forEach(note => {
        const title = note.querySelector('.title')?.textContent?.toLowerCase() || '';
        const content = note.querySelector('.content')?.textContent?.toLowerCase() || '';
        const noteTags = note.querySelectorAll('[data-tag-id]');

        // Tag-Filter
        let tagMatch = !tagId || tagId === '' || tagId === 'null';
        if (!tagMatch) {
            noteTags.forEach(tag => {
                if (tag.dataset.tagId === tagId) {
                    tagMatch = true;
                }
            });
        }

        // Text-Suche
        const textMatch = query ? (title.includes(query) || content.includes(query)) : true;

        if (tagMatch && textMatch) {
            note.style.display = 'block';
            if (query) highlightText(note, query);
            hasResults = true;
        } else {
            note.style.display = 'none';
            clearHighlight(note);
        }
    });

    if (!hasResults) {
        showNoResults();
    }
}

function showAllNotes() {
    const notes = document.querySelectorAll('#notesList > div');
    notes.forEach(note => {
        note.style.display = 'block';
        clearHighlight(note);
    });

    const noResults = document.querySelector('.no-results');
    if (noResults) noResults.remove();

    document.querySelectorAll('.tag-filter-item').forEach(i => i.classList.remove('active-tag-filter'));
    document.getElementById('selectedTagDisplay').textContent = 'Alle Tags';
}

function showNoResults() {
    const notesList = document.getElementById('notesList');
    if (!notesList) return;

    const existingNoResults = document.querySelector('.no-results');
    if (existingNoResults) existingNoResults.remove();

    const activeTag = document.querySelector('.tag-filter-item.active-tag-filter');
    const searchQuery = document.getElementById('searchInput')?.value?.trim() || '';
    let message = 'Keine Notizen gefunden';

    if (activeTag && searchQuery) {
        message = `Keine Notizen für "${activeTag.dataset.tagName}" mit "${searchQuery}" gefunden`;
    } else if (activeTag) {
        message = `Keine Notizen für "${activeTag.dataset.tagName}" gefunden`;
    } else if (searchQuery) {
        message = `Keine Notizen mit "${searchQuery}" gefunden`;
    }

    notesList.insertAdjacentHTML('beforeend',
        `<div class="col-12"><div class="no-results text-center p-5 text-muted">${message}</div></div>`
    );
}

function highlightText(note, query) {
    clearHighlight(note);

    if (!query) return;

    const title = note.querySelector('.title');
    if (title && title.textContent.toLowerCase().includes(query)) {
        highlightInElement(title, query);
    }

    const content = note.querySelector('.content');
    if (content && content.textContent.toLowerCase().includes(query)) {
        highlightInElement(content, query);
    }
}

function highlightInElement(element, query) {
    const text = element.textContent;
    const escapedQuery = query.replace("/[.*+?^${}()|[\$\\$/g, '\\$&'");
    const regex = new RegExp(`(${escapedQuery})`, 'gi');
    element.innerHTML = text.replace(regex, '<mark>$1</mark>');
}

function clearHighlight(note) {
    const highlights = note.querySelectorAll('mark');
    highlights.forEach(mark => {
        mark.outerHTML = mark.textContent;
    });
}

window.showAllNotes = showAllNotes;