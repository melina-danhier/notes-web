'use strict';

export function initSearchAndFilter() {
    const searchInput = document.getElementById('searchInput');
    const searchBtn = document.getElementById('searchBtn');

    if (!searchInput) return;

    // Search Event Listeners
    searchInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            performCombinedSearch();
        }
    });

    if (searchBtn) {
        searchBtn.addEventListener('click', performCombinedSearch);
    }

    // Tag Filter Event Listeners
    document.querySelectorAll('.tag-filter-item').forEach(item => {
        item.onclick = (e) => {
            e.preventDefault();
            e.stopPropagation();

            // Active Klasse togglen
            document.querySelectorAll('.tag-filter-item').forEach(i => i.classList.remove('active-tag-filter'));
            item.classList.add('active-tag-filter');

            const tagId = item.dataset.tagId;
            const tagName = item.dataset.tagName;

            // Display updaten
            document.getElementById('selectedTagDisplay').textContent = tagName;

            // Kombinierten Filter anwenden
            performCombinedSearch();
        };
    });

    // Debounce für Search Input (optional, für bessere Performance)
    let searchTimeout;
    searchInput.addEventListener('input', function() {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(performCombinedSearch, 300);
    });
}

function performCombinedSearch() {
    const query = document.getElementById('searchInput')?.value.toLowerCase().trim() || '';
    const activeTagFilter = document.querySelector('.tag-filter-item.active-tag-filter');
    const tagId = activeTagFilter ? activeTagFilter.dataset.tagId : null;

    const clearBtn = document.getElementById('clearSearchBtn');
    if (clearBtn) {
        clearBtn.style.display = query ? 'block' : 'none';
    }

    if (!query && !tagId) {
        showAllNotes();
        return;
    }

    // Notizen durchsuchen UND filtern
    const notes = document.querySelectorAll('#notesList > div');
    let hasResults = false;

    notes.forEach(note => {
        const title = note.querySelector('.title')?.textContent.toLowerCase() || '';
        const content = note.querySelector('.content')?.textContent.toLowerCase() || '';
        const noteTags = note.querySelectorAll('[data-tag-id]');

        // 1. Tag-Filter prüfen
        let tagMatch = !tagId || tagId === '' || tagId === 'null';
        if (!tagMatch) {
            noteTags.forEach(tag => {
                if (tag.dataset.tagId === tagId) {
                    tagMatch = true;
                }
            });
        }

        // 2. Text-Suche prüfen
        const textMatch = title.includes(query) || content.includes(query);

        // 3. BEIDE Bedingungen müssen erfüllt sein
        if (tagMatch && textMatch) {
            note.style.display = 'block';
            highlightText(note, query);
            hasResults = true;
        } else {
            note.style.display = 'none';
            clearHighlight(note);
        }
    });

    // Keine Ergebnisse anzeigen
    if (!hasResults) {
        showNoResults();
    }
}

function clearSearch() {
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.value = '';
        const clearBtn = document.getElementById('clearSearchBtn');
        if (clearBtn) clearBtn.style.display = 'none';
        showAllNotes();
        searchInput.focus();
    }
}

function showAllNotes() {
    const notes = document.querySelectorAll('#notesList > div');
    notes.forEach(note => {
        note.style.display = 'block';
        clearHighlight(note);
    });

    // No-results entfernen
    const noResults = document.querySelector('.no-results');
    if (noResults) noResults.remove();

    // Tag-Filter zurücksetzen (optional)
    document.querySelectorAll('.tag-filter-item').forEach(i => i.classList.remove('active-tag-filter'));
    document.getElementById('selectedTagDisplay').textContent = 'Alle Tags';
}

function showNoResults() {
    const notesList = document.getElementById('notesList');
    if (notesList) {
        // Bestehende No-Results entfernen
        const existingNoResults = document.querySelector('.no-results');
        if (existingNoResults) existingNoResults.remove();

        const activeTag = document.querySelector('.tag-filter-item.active-tag-filter');
        const searchQuery = document.getElementById('searchInput')?.value.trim() || '';
        let message = 'Keine Notizen gefunden';

        if (activeTag && searchQuery) {
            message = `Keine Notizen für "${activeTag.dataset.tagName}" mit "${searchQuery}" gefunden`;
        } else if (activeTag) {
            message = `Keine Notizen für "${activeTag.dataset.tagName}" gefunden`;
        } else if (searchQuery) {
            message = `Keine Notizen mit "${searchQuery}" gefunden`;
        }

        notesList.innerHTML += `<div class="col-12"><div class="no-results text-center p-5 text-muted">${message}</div></div>`;
    }
}

/** DEZENTE HERVORHEBUNG */
function highlightText(note, query) {
    clearHighlight(note);

    if (!query) return;

    // Title highlight
    const title = note.querySelector('h4');
    if (title) highlightInElement(title, query);

    // Content highlight
    const content = note.querySelector('.p-2');
    if (content) highlightInElement(content, query);
}

function highlightInElement(element, query) {
    const text = element.textContent;
    if (text.toLowerCase().includes(query)) {
        const regex = new RegExp(`(${query})`, 'gi');
        element.innerHTML = text.replace(regex, '<mark class="search-highlight-subtle">$1</mark>');
    }
}

function clearHighlight(note) {
    const highlights = note.querySelectorAll('.search-highlight-subtle');
    highlights.forEach(hl => {
        hl.outerHTML = hl.textContent;
    });
}

// Globale Funktionen für externe Verwendung
window.clearSearch = clearSearch;
window.performCombinedSearch = performCombinedSearch;
window.showAllNotes = showAllNotes;