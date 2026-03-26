'use strict';

/**
 * FILTER & SUCHE - KOMPLETT
 */
export function initSearch() {
    const input = document.getElementById('searchInput');
    if (input) input.oninput = () => filterNotes(input.value);
}

export function initTagFilter() {
    document.querySelectorAll('.tag-filter-item').forEach(item => {
        item.onclick = (e) => {
            e.preventDefault();

            document.querySelectorAll('.tag-filter-item').forEach(i => i.classList.remove('active-tag-filter'));
            item.classList.add('active-tag-filter');

            const tagName = item.dataset.tagName;
            const tagId = item.dataset.tagId;
            document.getElementById('selectedTagDisplay').textContent = tagName;
            filterNotesByTag(tagId);
        };
    });
}

// ✅ INTERN (nicht exportiert - nur innerhalb der Datei)
function filterNotesByTag(tagId) {
    document.querySelectorAll('[id^="note-"]').forEach(note => {
        const tags = note.querySelectorAll('span[class*="bg-primary"]');
        let match = !tagId || tagId === '' || tagId === 'null';

        if (!match) {
            tags.forEach(tag => {
                if (tag.dataset.tagId === tagId) match = true;
            });
        }

        note.style.display = match ? '' : 'none';
    });
}

// ✅ INTERN (nicht exportiert)
function filterNotes(searchTerm) {
    const notes = document.querySelectorAll('[id^="note-"]');
    const term = searchTerm.toLowerCase();

    notes.forEach(note => {
        const title = note.querySelector('h4')?.textContent.toLowerCase() || '';
        const content = note.querySelector('.p-2')?.textContent.toLowerCase() || '';

        note.style.display = (title.includes(term) || content.includes(term)) ? '' : 'none';
    });
}