'use strict';

/**
 * Markdown-Rendering und Note-Preview Modal Logic
 */

import { highlightText, highlightMarkdownContent } from './notes-utils.js';
import { getCurrentSearchTerm } from './notes.js';
import { renderMarkdownPreview, escapeHtml } from './markdown.js';

let currentEditNoteId = null;

export function initNotePreviewModal() {
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeNoteModal();
        }
    });

    const backdrop = document.querySelector('.note-view-modal-backdrop');
    if (backdrop) {
        backdrop.addEventListener('click', closeNoteModal);
    }
}

export async function viewNote(noteId) {
    try {
        const response = await fetch(`/api/notes/${noteId}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const note = await response.json();
        currentEditNoteId = noteId;
        displayNoteModal(note);
    } catch (error) {
        console.error('Fehler beim Laden der Notiz:', error);
        alert('Fehler beim Laden der Notiz: ' + error.message);
    }
}

export function displayNoteModal(note) {
    const modal = document.getElementById('noteViewModal');
    if (!modal) return;

    const searchTerm = getCurrentSearchTerm();
    
    const titleElement = document.getElementById('modalTitle');
    if (titleElement) {
        if (searchTerm) {
            titleElement.innerHTML = highlightText(note.title || '(Untitled)', searchTerm);
        } else {
            titleElement.textContent = note.title || '(Untitled)';
        }
    }

    const tagsContainer = document.getElementById('modalTags');
    if (tagsContainer && note.tags && note.tags.length > 0) {
        tagsContainer.innerHTML = note.tags
            .map(tag => `<span class="text-primary-emphasis bg-primary-subtle rounded-3 p-1">` +
                        `#${escapeHtml(tag.tag)}</span>`)
            .join('');
    } else {
        tagsContainer.innerHTML = '';
    }

    const contentContainer = document.getElementById('modalContent');
    if (contentContainer) {
        let html = renderMarkdownPreview(note.content || '');
        if (searchTerm) {
            html = highlightMarkdownContent(html, searchTerm);
        }
        contentContainer.innerHTML = html;
    }

    const timestamp = document.getElementById('modalTimestamp');
    if (timestamp) {
        let timestampHtml = '';

        if (note.created) {
            const createdDate = new Date(note.created);
            timestampHtml += `<div>Erstellt: ${createdDate.toLocaleString('de-DE', {
                day: '2-digit', month: '2-digit', year: 'numeric',
                hour: '2-digit', minute: '2-digit', hour12: false
            })}</div>`;
        }

        if (note.updated) {
            const updatedDate = new Date(note.updated);
            timestampHtml += `<div>Bearbeitet: ${updatedDate.toLocaleString('de-DE', {
                day: '2-digit', month: '2-digit', year: 'numeric',
                hour: '2-digit', minute: '2-digit', hour12: false
            })}</div>`;
        }

        timestamp.innerHTML = timestampHtml;
    }

    // Check if we're on notes page (edit button exists)
    const editBtn = document.getElementById('modalEditBtn');
    if (editBtn) {
        editBtn.onclick = () => {
            window.location.href = `/notes/${note.id}`;
        };
    }

     // Check if we're on notes page and add pin button functionality
     const pinBtn = document.getElementById('modalPinBtn');
     if (pinBtn) {
         pinBtn.style.display = 'block';
         pinBtn.dataset.noteId = note.id;
         pinBtn.dataset.pinned = note.pinned;
         const updatePinButtonText = (isPinned) => {
             pinBtn.innerHTML = isPinned ? '📍 Entfernen' : '📌 Anheften';
             pinBtn.className = `btn btn-${isPinned ? 'warning' : 'secondary'} btn-sm`;
             pinBtn.dataset.pinned = isPinned;
         };
         updatePinButtonText(note.pinned);
         pinBtn.onclick = () => {
             if (window.togglePin) {
                 window.togglePin(note.id, true);
             }
         };
     }

    // Check if we're on trash page (restore/delete buttons exist)
    const restoreBtn = document.getElementById('modalRestoreBtn');
    const deleteBtn = document.getElementById('modalDeleteBtn');
    if (restoreBtn && deleteBtn) {
        restoreBtn.onclick = () => {
            if (window.restoreNote) {
                window.restoreNote(note.id);
            } else {
                alert('Fehler: Wiederherstellen-Funktion nicht verfügbar');
            }
        };
        deleteBtn.onclick = () => {
            if (window.forceDeleteNote) {
                window.forceDeleteNote(note.id);
            } else {
                alert('Fehler: Löschen-Funktion nicht verfügbar');
            }
        };
    }

    modal.style.display = 'flex';
}

export function closeNoteModal() {
    const modal = document.getElementById('noteViewModal');
    if (modal) {
        modal.style.display = 'none';
        currentEditNoteId = null;
    }
}

export function closeNoteModalAndReload() {
    closeNoteModal();
    window.location.reload();
}

export function modalTogglePin() {
    const modal = document.getElementById('noteViewModal');
    if (modal && currentEditNoteId) {
        if (window.togglePin) {
            window.togglePin(currentEditNoteId, true);
        }
    }
}

export function editFromModal() {
    if (currentEditNoteId) {
        window.location.href = `/notes/${currentEditNoteId}`;
    }
}

// Markdown-Funktionen werden nun aus dem zentralen Modul importiert (./markdown.js)


