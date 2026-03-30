'use strict';

export function initSearchAndFilter() {
    const searchInput = document.getElementById('searchInput');
    if (!searchInput) return;

    // Note-Index einmal beim Init aufbauen
    let noteIndex = buildNoteIndex();
    let noResultsEl = null;

    searchInput.addEventListener('keypress', e => {
        if (e.key === 'Enter') { e.preventDefault(); performSearch(); }
    });

    let searchTimeout;
    searchInput.addEventListener('input', () => {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(performSearch, 700);
    });

    document.querySelectorAll('.tag-filter-item').forEach(item => {
        item.onclick = e => {
            e.preventDefault();
            e.stopPropagation();
            item.closest('.dropdown-menu')
                .querySelectorAll('.tag-filter-item')
                .forEach(i => i.classList.remove('active-tag-filter'));
            item.classList.add('active-tag-filter');
            document.getElementById('selectedTagDisplay').textContent = item.dataset.tagName;
            performSearch();
        };
    });

    function performSearch() {
        const query = searchInput.value.toLowerCase().trim();
        const activeTag = document.querySelector('.tag-filter-item.active-tag-filter');
        const tagId = activeTag?.dataset.tagId || '';

        hideNoResults();

        if (!query && !tagId) {
            noteIndex.forEach(({ el }) => {
                el.style.display = 'block';
                clearHighlight(el);
            });
            return;
        }

        let hasResults = false;

        noteIndex.forEach(({ el, title, content, tagIds }) => {
            const tagMatch = !tagId || tagIds.includes(tagId);
            const textMatch = !query || title.includes(query) || content.includes(query);

            if (tagMatch && textMatch) {
                el.style.display = 'block';
                if (query) highlightText(el, query); else clearHighlight(el);
                hasResults = true;
            } else {
                el.style.display = 'none';
                clearHighlight(el);
            }
        });

        if (!hasResults) showNoResults(activeTag, query);
    }

    function getOrCreateNoResults() {
        if (!noResultsEl) {
            noResultsEl = document.createElement('div');
            noResultsEl.className = 'col-12';
            noResultsEl.innerHTML = '<div class="no-results text-center p-5 text-muted"></div>';
            document.getElementById('notesList')?.appendChild(noResultsEl);
        }
        return noResultsEl;
    }

    function showNoResults(activeTag, query) {
        let message = 'Keine Notizen gefunden';
        if (activeTag && query) {
            message = `Keine Notizen für "${activeTag.dataset.tagName}" mit "${query}"`;
        } else if (activeTag) {
            message = `Keine Notizen für "${activeTag.dataset.tagName}"`;
        } else if (query) {
            message = `Keine Notizen mit "${query}" gefunden`;
        }
        const el = getOrCreateNoResults();
        el.querySelector('.no-results').textContent = message;
        el.style.display = 'block';
    }

    function hideNoResults() {
        if (noResultsEl) noResultsEl.style.display = 'none';
    }

    window.showAllNotes = function () {
        noteIndex.forEach(({ el }) => {
            el.style.display = 'block';
            clearHighlight(el);
        });
        hideNoResults();
        document.querySelectorAll('.tag-filter-item').forEach(i => i.classList.remove('active-tag-filter'));
        document.getElementById('selectedTagDisplay').textContent = 'Alle Tags';
    };
}

function buildNoteIndex() {
    return [...document.querySelectorAll('#notesList > div')].map(note => ({
        el: note,
        title: note.querySelector('.title')?.textContent?.toLowerCase() || '',
        content: note.querySelector('.content')?.textContent?.toLowerCase() || '',
        tagIds: [...note.querySelectorAll('[data-tag-id]')].map(t => t.dataset.tagId)
    }));
}

function highlightText(note, query) {
    clearHighlight(note);
    const escaped = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const regex = new RegExp(`(${escaped})`, 'gi');
    ['.title', '.content'].forEach(sel => {
        const el = note.querySelector(sel);
        if (el?.textContent.toLowerCase().includes(query)) {
            el.innerHTML = el.textContent.replace(regex, '<mark>$1</mark>');
        }
    });
}

function clearHighlight(note) {
    note.querySelectorAll('mark').forEach(mark => {
        mark.replaceWith(mark.firstChild);
    });
}