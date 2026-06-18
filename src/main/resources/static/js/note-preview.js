'use strict';

import { getCsrfToken, getCsrfHeader, highlightText, highlightMarkdownContent } from './notes-utils.js';
import { renderMarkdownPreview, escapeHtml } from './markdown.js';

let currentNote = null;

export function initNotePreviewModal() {
    const modal = document.getElementById('noteViewModal');
    if (!modal) return;

    modal.querySelector('.note-view-modal-backdrop')?.addEventListener('click', closeNoteModal);
    modal.querySelector('#modalContent')?.addEventListener('change', handleTaskToggle);

    document.addEventListener('keydown', event => {
        if (event.key === 'Escape' && modal.style.display !== 'none') {
            closeNoteModal();
        }
    });
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
    currentNote = note;
    
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
                        `#${escapeHtml(tag)}</span>`)
            .join('');
    } else {
        tagsContainer.innerHTML = '';
    }

    const contentContainer = document.getElementById('modalContent');
    if (contentContainer) {
        const interactiveTasks = !document.getElementById('modalRestoreBtn');
        let html = renderMarkdownPreview(note.content || '', { interactiveTasks });
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

    const editBtn = document.getElementById('modalEditBtn');
    if (editBtn) {
        editBtn.onclick = editFromModal;
    }

    const archiveBtn = document.getElementById('modalArchiveBtn');
    if (archiveBtn) {
        archiveBtn.onclick = () => {
            if (window.archiveNote) {
                window.archiveNote(note.id);
            } else {
                alert('Fehler: Archivieren-Funktion nicht verfügbar');
            }
        };
    }

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
        pinBtn.onclick = modalTogglePin;
    }

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

    const unarchiveBtn = document.getElementById('modalUnarchiveBtn');
    const archiveDeleteBtn = document.getElementById('modalArchiveDeleteBtn');
    if (unarchiveBtn && archiveDeleteBtn) {
        unarchiveBtn.onclick = () => {
            if (window.unarchiveNote) {
                window.unarchiveNote(note.id);
            } else {
                alert('Fehler: Wiederherstellen-Funktion nicht verfügbar');
            }
        };
        archiveDeleteBtn.onclick = () => {
            if (window.deleteArchivedNote) {
                window.deleteArchivedNote(note.id);
            } else {
                alert('Fehler: Löschen-Funktion nicht verfügbar');
            }
        };
    }

    modal.style.display = 'flex';
}

export function editFromModal() {
    if (currentNote?.id) {
        window.location.href = `/notes/${currentNote.id}`;
    }
}

export function modalTogglePin() {
    if (currentNote?.id && window.togglePin) {
        window.togglePin(currentNote.id, true);
    }
}

async function handleTaskToggle(changeEvent) {
    const checkbox = changeEvent.target.closest('input[type="checkbox"][data-task-index]');
    if (!checkbox || !currentNote) return;

    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();
    if (!csrfToken || !csrfHeader) {
        checkbox.checked = !checkbox.checked;
        alert('Sicherheitstoken fehlt. Bitte Seite neu laden.');
        return;
    }

    checkbox.disabled = true;
    try {
        const response = await fetch(
            `/api/notes/${currentNote.id}/tasks/${checkbox.dataset.taskIndex}/toggle`,
            {
                method: 'PATCH',
                headers: { [csrfHeader]: csrfToken }
            }
        );
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        displayNoteModal(await response.json());
    } catch (error) {
        checkbox.checked = !checkbox.checked;
        checkbox.disabled = false;
        alert('Aufgabe konnte nicht gespeichert werden.');
        console.error('Fehler beim Speichern der Aufgabe:', error);
    }
}

function getCurrentSearchTerm() {
    return new URL(window.location.href).searchParams.get('search');
}

export function closeNoteModal() {
    const modal = document.getElementById('noteViewModal');
    if (modal) {
        modal.style.display = 'none';
        currentNote = null;
    }
}

export function closeNoteModalAndReload() {
    closeNoteModal();
    window.location.reload();
}
