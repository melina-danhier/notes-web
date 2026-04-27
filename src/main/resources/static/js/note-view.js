'use strict';

document.addEventListener('DOMContentLoaded', function() {
    initEditNotesApp();
});

function initEditNotesApp() {
    const saveBtn = document.getElementById('saveBtn');
    if (!saveBtn) {
        console.error('Save Button (#saveBtn) nicht gefunden!');
        return;
    }

    const titleInput = document.getElementById('noteIdInput') || null;
    if (titleInput) titleInput.focus();

    initSaveButton();
    initAutoResize();
    initKeyboardShortcuts();
    initFormValidation();
}

function initSaveButton() {
    const saveBtn = document.getElementById('saveBtn');
    if (!saveBtn) return;

    saveBtn.addEventListener('click', function(e) {
        e.preventDefault();
        saveNote();
    });
}

function saveNote() {
    const saveBtn = document.getElementById('saveBtn');
    if (!saveBtn) return;

    const formData = getFormData();
    if (!isValidForm(formData)) return;

    showLoading(saveBtn, '💾 Speichern...');

    const csrfToken = getCsrfToken();
    const csrfHeader = getCsrfHeader();

    if (!csrfToken || !csrfHeader) {
        showError('Sicherheitstoken fehlt. Bitte Seite neu laden.');
        return;
    }

    const noteId = formData.id;
    const url = noteId ? `/api/notes/${noteId}` : '/api/notes';
    const method = noteId ? 'PUT' : 'POST';

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify(formData)
    })
        .then(response => {
            if (response.ok) {
                showSuccess('✅ Notiz gespeichert!');
                setTimeout(() => window.location.href = '/notes', 1200);
            } else {
                return response.text().then(text => {
                    throw new Error(`HTTP ${response.status}: ${text}`);
                });
            }
        })
        .catch(error => {
            console.error('Save Error:', error);
            showError(`❌ Fehler: ${error.message}`);
            hideLoading(saveBtn);
        });
}

function getFormData() {
    return {
        id: document.getElementById('noteIdInput')?.value || null,
        title: document.getElementById('titleInput')?.value?.trim() || '',
        content: document.getElementById('contentInput')?.value || '',
        tagsRaw: document.getElementById('tagsInput')?.value?.trim() || ''
    };
}

function isValidForm(data) {
    if (!data.title) {
        showError('Titel ist erforderlich!');
        document.getElementById('titleInput')?.focus();
        return false;
    }
    if (data.title.length > 200) {
        showError('Titel zu lang (max 200 Zeichen)!');
        return false;
    }
    return true;
}

function initAutoResize() {
    const textarea = document.getElementById('contentInput');
    if (!textarea) return;

    textarea.addEventListener('input', function() {
        this.style.height = 'auto';
        this.style.height = Math.min(this.scrollHeight, 400) + 'px';
    });

    textarea.dispatchEvent(new Event('input'));
}

function initKeyboardShortcuts() {
    document.addEventListener('keydown', function(e) {
        if ((e.ctrlKey || e.metaKey) && e.key === 's') {
            e.preventDefault();
            saveNote();
            return;
        }

        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
            e.preventDefault();
            saveNote();
            return;
        }

        if (e.key === 'Escape') {
            if (confirm('Änderungen verwerfen und zurück?')) {
                window.history.back();
            }
        }
    });
}

function initFormValidation() {
    const titleInput = document.getElementById('titleInput');
    if (!titleInput) return;

    titleInput.addEventListener('blur', function() {
        if (!this.value.trim()) {
            showError('Titel ist erforderlich!');
        }
    });
}

function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
}

function getCsrfHeader() {
    return document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
}

function showLoading(button, text) {
    const originalText = button.innerHTML;
    const originalDisabled = button.disabled;

    button.dataset.originalText = originalText;
    button.disabled = true;
    button.innerHTML = text;

    return { originalText, originalDisabled };
}

function hideLoading(button) {
    if (button.dataset.originalText) {
        button.innerHTML = button.dataset.originalText;
        button.disabled = false;
        delete button.dataset.originalText;
    }
}

function showSuccess(msg) {
    showNotification(msg, 'success');
}

function showError(msg) {
    showNotification(msg, 'error');
}

function showNotification(msg, type) {
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
    toastDiv.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${msg}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;

    toastContainer.appendChild(toastDiv);

    const toast = new bootstrap.Toast(toastDiv);
    toast.show();

    setTimeout(() => {
        if (toastDiv.parentNode) {
            toastDiv.remove();
        }
    }, 4000);
}