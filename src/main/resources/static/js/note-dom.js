'use strict';

export function removeNoteCard(noteId, listId, emptyStateId) {
    document.getElementById(`note-${noteId}`)?.remove();
    closeNoteModal();
    updateEmptyState(listId, emptyStateId);
}

export function removeNoteFromMainPage(noteId) {
    document.getElementById(`note-${noteId}`)?.remove();
    closeNoteModal();
    updateMainPageSections();
}

export function clearNoteList(listId, emptyStateId) {
    const list = document.getElementById(listId);
    if (list) {
        list.replaceChildren();
    }
    closeNoteModal();
    updateEmptyState(listId, emptyStateId);
}

export function moveNoteCardForPin(noteId, pinned) {
    const cardColumn = document.getElementById(`note-${noteId}`);
    const targetList = document.getElementById(pinned ? 'pinnedNotesList' : 'notesList');
    if (!cardColumn || !targetList) {
        return;
    }

    targetList.prepend(cardColumn);
    updateCardPinAppearance(cardColumn, pinned);
    updateMainPageSections();
}

function updateCardPinAppearance(cardColumn, pinned) {
    const card = cardColumn.querySelector('.card');
    const titleRow = cardColumn.querySelector('.card-header > .d-flex');
    const pinButton = cardColumn.querySelector('.pin-btn');

    card?.classList.toggle('pinned-card', pinned);

    let pinIcon = titleRow?.querySelector('.pin-icon');
    if (pinned && titleRow && !pinIcon) {
        pinIcon = document.createElement('span');
        pinIcon.className = 'pin-icon';
        pinIcon.title = 'Angeheftet';
        pinIcon.textContent = '📌';
        titleRow.prepend(pinIcon);
    } else if (!pinned) {
        pinIcon?.remove();
    }

    if (pinButton) {
        pinButton.className = pinned ? 'btn btn-warning pin-btn' : 'btn btn-secondary pin-btn';
        pinButton.title = pinned ? 'Trennen' : 'Anheften';
        pinButton.dataset.pinned = String(pinned);
        const icon = pinButton.querySelector('span');
        if (icon) {
            icon.textContent = pinned ? '📍' : '📌';
        }
        pinButton.disabled = false;
    }
}

function updateMainPageSections() {
    const pinnedList = document.getElementById('pinnedNotesList');
    const notesList = document.getElementById('notesList');
    const pinnedSection = document.getElementById('pinnedNotesSection');
    const emptyState = document.getElementById('notesEmptyState');

    const hasPinnedNotes = hasCards(pinnedList);
    const hasNotes = hasCards(notesList);

    if (pinnedSection) {
        pinnedSection.style.display = hasPinnedNotes ? '' : 'none';
    }
    if (emptyState) {
        emptyState.style.display = hasPinnedNotes || hasNotes ? 'none' : '';
    }
}

function updateEmptyState(listId, emptyStateId) {
    const emptyState = document.getElementById(emptyStateId);
    if (emptyState) {
        emptyState.style.display = hasCards(document.getElementById(listId)) ? 'none' : '';
    }
}

function hasCards(list) {
    return Boolean(list?.querySelector('[id^="note-"]'));
}

function closeNoteModal() {
    const modal = document.getElementById('noteViewModal');
    if (modal) {
        modal.style.display = 'none';
    }
}
