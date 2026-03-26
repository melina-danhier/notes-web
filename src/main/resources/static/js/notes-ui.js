'use strict';

/**
 * UI - Toast, Animationen
 */
export function showSuccessToast(message) {
    let toast = document.getElementById('successToast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'successToast';
        toast.className = 'toast align-items-center text-white bg-success border-0 position-fixed top-0 end-0 m-3';
        toast.role = 'alert';
        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        `;
        document.body.appendChild(toast);
    }
    new bootstrap.Toast(toast).show();
}

export function checkEmptyState() {
    const notesList = document.getElementById('notesList');
    const notes = document.querySelectorAll('[id^="note-"]');

    if (notes.length === 0 && notesList) {
        notesList.innerHTML = `
            <div class="col-12 text-center py-5">
                <h5 class="fst-italic text-muted">Keine Notizen vorhanden</h5>
                <p class="text-muted">➕ Erstelle deine erste Notiz!</p>
            </div>
        `;
    }
}