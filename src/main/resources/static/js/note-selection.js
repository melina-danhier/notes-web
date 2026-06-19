'use strict';

import { getCsrfHeader, getCsrfToken } from './notes-utils.js';

export function initNoteSelection({ actions }) {
    const bulkBar = document.getElementById('bulkActionBar');
    const countLabel = document.getElementById('selectedNoteCount');
    const clearButton = document.getElementById('clearSelectionBtn');
    const checkboxes = Array.from(document.querySelectorAll('.note-select-checkbox'));

    if (!bulkBar || !countLabel || checkboxes.length === 0) {
        return;
    }

    checkboxes.forEach(checkbox => {
        checkbox.addEventListener('click', event => event.stopPropagation());
        checkbox.addEventListener('change', updateSelectionState);
    });

    clearButton?.addEventListener('click', () => {
        checkboxes.forEach(checkbox => {
            checkbox.checked = false;
        });
        updateSelectionState();
    });

    document.querySelectorAll('[data-bulk-action]').forEach(button => {
        button.addEventListener('click', () => runBulkAction(button.dataset.bulkAction, button));
    });

    updateSelectionState();

    function updateSelectionState() {
        const selectedCount = getSelectedIds().length;
        countLabel.textContent = String(selectedCount);
        bulkBar.classList.toggle('d-none', selectedCount === 0);

        checkboxes.forEach(checkbox => {
            checkbox.closest('[id^="note-"]')?.classList.toggle('is-selected', checkbox.checked);
        });
    }

    async function runBulkAction(actionName, button) {
        const action = actions[actionName];
        const noteIds = getSelectedIds();

        if (!action || noteIds.length === 0) {
            return;
        }

        if (action.confirmMessage && !confirm(action.confirmMessage(noteIds.length))) {
            return;
        }

        setBulkButtonsDisabled(true);
        button.classList.add('disabled');

        try {
            for (const noteId of noteIds) {
                await sendNoteAction(noteId, action);
            }
            window.location.reload();
        } catch (error) {
            console.error('Mehrfachaktion fehlgeschlagen:', error);
            alert(action.errorMessage || 'Mehrfachaktion fehlgeschlagen');
            setBulkButtonsDisabled(false);
            button.classList.remove('disabled');
        }
    }

    function getSelectedIds() {
        return checkboxes
            .filter(checkbox => checkbox.checked)
            .map(checkbox => checkbox.value);
    }
}

async function sendNoteAction(noteId, action) {
    const response = await fetch(action.url(noteId), {
        method: action.method,
        headers: { [getCsrfHeader()]: getCsrfToken() }
    });

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }
}

function setBulkButtonsDisabled(disabled) {
    document.querySelectorAll('[data-bulk-action], #clearSelectionBtn').forEach(button => {
        button.disabled = disabled;
    });
}
