'use strict';

import { getCsrfHeader, getCsrfToken } from './notes-utils.js';

export function initNoteSelection({ actions }) {
    const bulkBar = document.getElementById('bulkActionBar');
    const countLabel = document.getElementById('selectedNoteCount');
    const clearButton = document.getElementById('clearSelectionBtn');
    const checkboxes = Array.from(document.querySelectorAll('.note-select-checkbox'));
    const mobileQuery = window.matchMedia('(max-width: 576px)');
    let mobileSelectionMode = false;

    if (!bulkBar || !countLabel || checkboxes.length === 0) {
        return;
    }

    checkboxes.forEach(checkbox => {
        checkbox.addEventListener('click', event => event.stopPropagation());
        checkbox.addEventListener('change', updateSelectionState);
    });

    initMobileLongPressSelection();

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
        mobileSelectionMode = mobileQuery.matches && selectedCount > 0;
        document.body.classList.toggle('mobile-selection-mode', mobileSelectionMode);
        countLabel.textContent = String(selectedCount);
        bulkBar.classList.toggle('d-none', selectedCount === 0);

        checkboxes.forEach(checkbox => {
            checkbox.closest('[id^="note-"]')?.classList.toggle('is-selected', checkbox.checked);
        });
    }

    function initMobileLongPressSelection() {
        document.querySelectorAll('.click-card[data-note-id]').forEach(card => {
            const checkbox = card.querySelector('.note-select-checkbox');
            if (!checkbox) return;

            let timer = null;
            let startX = 0;
            let startY = 0;
            let suppressNextClick = false;

            const cancelTimer = () => {
                if (timer !== null) {
                    window.clearTimeout(timer);
                    timer = null;
                }
            };

            const finishPress = () => {
                cancelTimer();
                if (suppressNextClick) {
                    window.setTimeout(() => {
                        suppressNextClick = false;
                    }, 500);
                }
            };

            card.addEventListener('pointerdown', event => {
                if (!mobileQuery.matches || event.button !== 0 || event.target.closest('button, a, input, label')) return;

                startX = event.clientX;
                startY = event.clientY;
                cancelTimer();
                timer = window.setTimeout(() => {
                    timer = null;
                    suppressNextClick = true;
                    checkbox.checked = true;
                    updateSelectionState();
                    navigator.vibrate?.(35);
                }, 550);
            });

            card.addEventListener('pointermove', event => {
                if (Math.abs(event.clientX - startX) > 10 || Math.abs(event.clientY - startY) > 10) cancelTimer();
            });

            card.addEventListener('pointerup', finishPress);
            card.addEventListener('pointercancel', finishPress);
            card.addEventListener('pointerleave', finishPress);
            card.addEventListener('contextmenu', event => {
                if (mobileQuery.matches) event.preventDefault();
            });

            card.addEventListener('click', event => {
                if (!mobileQuery.matches) return;

                if (suppressNextClick) {
                    suppressNextClick = false;
                    event.preventDefault();
                    event.stopImmediatePropagation();
                    return;
                }

                if (mobileSelectionMode) {
                    event.preventDefault();
                    event.stopImmediatePropagation();
                    checkbox.checked = !checkbox.checked;
                    updateSelectionState();
                }
            }, true);
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
