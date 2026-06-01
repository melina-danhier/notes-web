'use strict';

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

// Allgemeine Benachrichtigungsfunktionen, die auch von anderen Skripten genutzt werden
export function showNotification(msg, type) {
    let toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toastContainer';
        toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        document.body.appendChild(toastContainer);
    }

    const toastDiv = document.createElement('div');
    toastDiv.className = `toast align-items-center text-white bg-${type === 'success' ? 'success' : 'danger'} border-0 shadow-lg`;
    toastDiv.setAttribute('role', 'alert');
    toastDiv.setAttribute('aria-live', 'assertive');
    toastDiv.setAttribute('aria-atomic', 'true');

    const toastBodyWrapper = document.createElement('div');
    toastBodyWrapper.className = 'd-flex';

    const toastBody = document.createElement('div');
    toastBody.className = 'toast-body';
    toastBody.textContent = msg;

    toastBodyWrapper.appendChild(toastBody);
    toastDiv.appendChild(toastBodyWrapper);
    toastContainer.appendChild(toastDiv);

    const toast = new bootstrap.Toast(toastDiv);
    toast.show();

    setTimeout(() => { if (toastDiv.parentNode) { toastDiv.remove(); } }, 4000);
}

export function showSuccess(msg) { showNotification(msg, 'success'); }
export function showError(msg) { showNotification(msg, 'error'); }
