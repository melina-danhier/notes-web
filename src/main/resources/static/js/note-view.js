'use strict';
import { getCsrfToken, getCsrfHeader } from './notes-utils.js';
import { showSuccess, showError } from './notes-ui.js';
import { renderMarkdownPreview, escapeHtml } from './markdown.js';

document.addEventListener('DOMContentLoaded', function() {
    initEditNotesApp();
});

function initEditNotesApp() {
    const saveBtn = document.getElementById('saveBtn');
    if (!saveBtn) {
        console.error('Save Button (#saveBtn) nicht gefunden!');
        return;
    }

    document.getElementById('titleInput')?.focus();

    initSaveButton();
    initMarkdownPreview();
    initMarkdownToolbar();
    initToolbarExtrasToggle();
    initToggleButtons();
    initSplitResize();
    initTagInput();
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
        hideLoading(saveBtn);
        showError('Sicherheitstoken fehlt. Bitte Seite neu laden.');
        return;
    }

    const noteId = formData.id;
    const url = noteId ? `/api/notes/${noteId}` : '/api/notes';
    const method = noteId ? 'PATCH' : 'POST';

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
        tags: parseTagsField(document.getElementById('tagsInput')?.value)
    };
}

function isValidForm(data) {
    // Titel ist optional. Nur Länge validieren, falls gesetzt.
    if (data.title && data.title.length > 30) {
        showError('Titel zu lang (max 30 Zeichen)!');
        return false;
    }
    return true;
}

function initMarkdownPreview() {
    const textarea = document.getElementById('contentInput');
    const preview = document.getElementById('markdownPreview');

    if (!textarea || !preview) return;

    const updatePreview = function() {
        preview.innerHTML = renderMarkdownPreview(textarea.value);
    };

    textarea.addEventListener('input', updatePreview);
    updatePreview();
}

function initMarkdownToolbar() {
    const toolbar = document.querySelector('.markdown-toolbar');
    const textarea = document.getElementById('contentInput');

    if (!toolbar || !textarea) return;

    toolbar.addEventListener('click', function(clickEvent) {
        const button = clickEvent.target.closest('button[data-md-action]');
        if (!button) return;

        clickEvent.preventDefault();
        handleMarkdownAction(button.dataset.mdAction, textarea);
    });
}

function handleMarkdownAction(action, textarea) {
    switch (action) {
        case 'heading-1':
            prefixMarkdownLines(textarea, '# ');
            break;
        case 'heading-2':
            prefixMarkdownLines(textarea, '## ');
            break;
        case 'heading-3':
            prefixMarkdownLines(textarea, '### ');
            break;
        case 'bold':
            insertMarkdownWrap(textarea, '**', '**', 'fett');
            break;
        case 'italic':
            insertMarkdownWrap(textarea, '_', '_', 'kursiv');
            break;
        case 'link':
            insertMarkdownLink(textarea);
            break;
        case 'bullet-list':
            prefixMarkdownLines(textarea, '- ');
            break;
        case 'ordered-list':
            prefixMarkdownLines(textarea, '1. ', { ordered: true });
            break;
        case 'task':
            insertMarkdownWrap(textarea, '- [ ] ', '', 'Aufgabe');
            break;
        case 'toggle':
            insertMarkdownToggleStart(textarea);
            break;
        case 'toggle-end':
            insertMarkdownToggleEnd(textarea);
            break;
        default:
            console.warn(`Unbekannte Markdown-Aktion: ${action}`);
    }
}

function insertMarkdownToggleStart(textarea) {
    const start = textarea.selectionStart ?? 0;
    const end = textarea.selectionEnd ?? 0;
    const value = textarea.value;
    const title = value.slice(start, end) || 'Titel';
    const leadingBreak = start > 0 && value[start - 1] !== '\n' ? '\n' : '';
    const replacement = `${leadingBreak}:::toggle ${title}\n`;
    const titleStart = start + leadingBreak.length + ':::toggle '.length;

    textarea.value = value.slice(0, start) + replacement + textarea.value.slice(end);
    textarea.focus();
    textarea.setSelectionRange(titleStart, titleStart + title.length);
    triggerPreviewRefresh(textarea);
}

function insertMarkdownToggleEnd(textarea) {
    const start = textarea.selectionStart ?? 0;
    const end = textarea.selectionEnd ?? 0;
    const value = textarea.value;
    const leadingBreak = start > 0 && value[start - 1] !== '\n' ? '\n' : '';
    const trailingBreak = end < value.length && value[end] !== '\n' ? '\n' : '';
    const replacement = `${leadingBreak}:::\n${trailingBreak}`;

    textarea.value = value.slice(0, start) + replacement + value.slice(end);
    textarea.focus();
    textarea.setSelectionRange(start + replacement.length, start + replacement.length);
    triggerPreviewRefresh(textarea);
}

function insertMarkdownWrap(textarea, before, after, placeholder) {
    const start = textarea.selectionStart ?? 0;
    const end = textarea.selectionEnd ?? 0;
    const selection = textarea.value.slice(start, end);
    const content = selection || placeholder;
    const replacement = `${before}${content}${after}`;

    textarea.value = textarea.value.slice(0, start) + replacement + textarea.value.slice(end);
    textarea.focus();
    textarea.setSelectionRange(start + before.length, start + before.length + content.length);
    triggerPreviewRefresh(textarea);
}

function prefixMarkdownLines(textarea, prefix, options = {}) {
    const start = textarea.selectionStart ?? 0;
    const end = textarea.selectionEnd ?? 0;
    const value = textarea.value;
    const hasSelection = start !== end;
    const lineStart = value.lastIndexOf('\n', start - 1) + 1;
    const lineEndBreak = value.indexOf('\n', end);
    const lineEnd = lineEndBreak === -1 ? value.length : lineEndBreak;
    const block = value.slice(lineStart, lineEnd);

    if (!hasSelection) {
        const currentLine = block;
        if (options.ordered) {
            const existingOrderedPrefix = currentLine.match(/^\d+\.\s+/)?.[0];
            if (existingOrderedPrefix) {
                textarea.focus();
                textarea.setSelectionRange(start + existingOrderedPrefix.length, start + existingOrderedPrefix.length);
                return;
            }
        } else if (currentLine.startsWith(prefix)) {
            textarea.focus();
            textarea.setSelectionRange(start + prefix.length, start + prefix.length);
            return;
        }
    }

    const modified = block.split('\n').map(function(line, index) {
        if (options.ordered) {
            return `${index + 1}. ${line.replace(/^\d+\.\s+/, '')}`;
        }

        return line.startsWith(prefix) ? line : `${prefix}${line}`;
    }).join('\n');

    textarea.value = value.slice(0, lineStart) + modified + value.slice(lineEnd);
    textarea.focus();

    if (hasSelection) {
        textarea.setSelectionRange(lineStart, lineStart + modified.length);
    } else {
        textarea.setSelectionRange(start + prefix.length, start + prefix.length);
    }

    triggerPreviewRefresh(textarea);
}

function insertMarkdownLink(textarea) {
    const start = textarea.selectionStart ?? 0;
    const end = textarea.selectionEnd ?? 0;
    const value = textarea.value;
    const selectedText = value.slice(start, end).trim();
    const url = window.prompt('Link-URL eingeben', 'https://');

    if (url === null) return;

    const trimmedUrl = url.trim();
    if (!trimmedUrl) {
        showError('Bitte eine gültige Link-URL eingeben.');
        return;
    }

    const label = selectedText || window.prompt('Anzeigetext eingeben', 'Link-Text');
    if (!label || !label.trim()) {
        showError('Bitte einen Link-Text eingeben.');
        return;
    }

    const replacement = `[${label.trim()}](${trimmedUrl})`;
    textarea.value = value.slice(0, start) + replacement + value.slice(end);
    textarea.focus();
    textarea.setSelectionRange(start + 1, start + 1 + label.trim().length);
    triggerPreviewRefresh(textarea);
}

function triggerPreviewRefresh(textarea) {
    textarea.dispatchEvent(new Event('input', { bubbles: true }));
}

// escapeHtml wird aus dem Markdown-Modul importiert

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

    // Keine Pflichtvalidierung für den Titel mehr; Länge wird beim Speichern geprüft.
    titleInput.addEventListener('blur', function() {
        if (this.value && this.value.trim().length > 30) {
            showError('Titel zu lang (max 30 Zeichen)!');
        }
    });
}
// CSRF-Token werden über das shared notes-utils Modul bezogen

function showLoading(button, text) {
    const originalText = button.innerHTML;

    button.dataset.originalText = originalText;
    button.disabled = true;
    button.innerHTML = text;

    return { originalText };
}

function hideLoading(button) {
    if (button.dataset.originalText) {
        button.innerHTML = button.dataset.originalText;
        button.disabled = false;
        delete button.dataset.originalText;
    }
}

// Benachrichtigungen werden über './notes-ui.js' bereitgestellt

// ======================== TOGGLE BUTTONS ========================

function initToggleButtons() {
    const toggleToolbar = document.getElementById('toggleToolbar');
    const togglePreview = document.getElementById('togglePreview');
    const toolbarContainer = document.getElementById('toolbarContainer');
    const previewContainer = document.getElementById('previewContainer');
    const splitRoot = document.getElementById('splitRoot');
    const editorColumn = document.getElementById('editorColumn');

    if (toggleToolbar && toolbarContainer) {
        if (localStorage.getItem('toolbarVisible') === null) {
            localStorage.setItem('toolbarVisible', 'false');
        }

        toggleToolbar.addEventListener('click', function(e) {
            e.preventDefault();
            const isHidden = toolbarContainer.style.display === 'none';
            toolbarContainer.style.display = isHidden ? 'block' : 'none';
            toggleToolbar.classList.toggle('collapsed', !isHidden);
            localStorage.setItem('toolbarVisible', isHidden ? 'true' : 'false');
        });

        // Toolbar standardmäßig versteckt
        const toolbarVisible = localStorage.getItem('toolbarVisible') === 'true';
        if (toolbarVisible) {
            toolbarContainer.style.display = 'block';
        } else {
            toggleToolbar.classList.add('collapsed');
        }
    }

    if (togglePreview && previewContainer && splitRoot) {
        if (localStorage.getItem('previewVisible') === null) {
            localStorage.setItem('previewVisible', 'false');
        }

        const applyPreviewState = function(showPreview) {
            previewContainer.style.display = showPreview ? 'block' : 'none';
            splitRoot.classList.toggle('preview-open', showPreview);
            togglePreview.classList.toggle('collapsed', !showPreview);

            if (editorColumn) {
                editorColumn.classList.toggle('fullwidth', !showPreview);
            }
        };

        togglePreview.addEventListener('click', function(e) {
            e.preventDefault();
            const showPreview = previewContainer.style.display === 'none';
            applyPreviewState(showPreview);
            localStorage.setItem('previewVisible', showPreview ? 'true' : 'false');
        });

        // Preview standardmäßig versteckt
        const previewVisible = localStorage.getItem('previewVisible') === 'true';
        applyPreviewState(previewVisible);
    }
}

function initToolbarExtrasToggle() {
    const toolbarContainer = document.getElementById('toolbarContainer');
    const toggleExtras = document.getElementById('toggleToolbarExtras');
    const extras = document.getElementById('toolbarExtras');

    if (!toolbarContainer || !toggleExtras || !extras) return;

    const applyState = function(isExpanded) {
        toolbarContainer.classList.toggle('toolbar-extras-expanded', isExpanded);
        toggleExtras.setAttribute('aria-expanded', String(isExpanded));
        toggleExtras.classList.toggle('collapsed', !isExpanded);
    };

    toggleExtras.addEventListener('click', function(e) {
        e.preventDefault();
        applyState(!toolbarContainer.classList.contains('toolbar-extras-expanded'));
    });

    applyState(false);
}

// ======================== SPLIT RESIZE ========================

function initSplitResize() {
    const splitRoot = document.getElementById('splitRoot');
    const splitDivider = document.getElementById('splitDivider');
    const previewContainer = document.getElementById('previewContainer');

    if (!splitRoot || !splitDivider || !previewContainer) return;

    const minRatio = 0.25;
    const maxRatio = 0.75;
    let isDragging = false;

    const storedRatio = parseFloat(localStorage.getItem('noteEditorSplitRatio') || '');
    setSplitRatio(splitRoot, Number.isFinite(storedRatio) ? storedRatio : 0.55);

    splitDivider.addEventListener('pointerdown', function(pointerEvent) {
        if (previewContainer.style.display === 'none') return;

        isDragging = true;
        splitDivider.setPointerCapture(pointerEvent.pointerId);
        document.body.classList.add('split-resizing');
        pointerEvent.preventDefault();
    });

    document.addEventListener('pointermove', function(pointerEvent) {
        if (!isDragging) return;

        const rect = splitRoot.getBoundingClientRect();
        const dividerWidth = parseFloat(getComputedStyle(splitRoot).getPropertyValue('--split-divider-width')) || 12;
        const availableWidth = Math.max(rect.width - dividerWidth, 1);
        const previewWidth = pointerEvent.clientX - rect.left;
        const ratio = clamp(previewWidth / availableWidth, minRatio, maxRatio);

        setSplitRatio(splitRoot, ratio);
        localStorage.setItem('noteEditorSplitRatio', ratio.toFixed(3));
    });

    const stopDragging = function() {
        if (!isDragging) return;
        isDragging = false;
        document.body.classList.remove('split-resizing');
    };

    document.addEventListener('pointerup', stopDragging);
    document.addEventListener('pointercancel', stopDragging);
}

function setSplitRatio(splitRoot, ratio) {
    splitRoot.style.setProperty('--split-ratio', String(ratio));
}

function clamp(value, min, max) {
    return Math.min(Math.max(value, min), max);
}

// ======================== TAG INPUT SYSTEM ========================

function parseTagsField(rawValue) {
    if (!rawValue) {
        return [];
    }

    const normalizedValue = rawValue.trim();
    if (!normalizedValue) {
        return [];
    }

    try {
        const parsed = JSON.parse(normalizedValue);
        if (Array.isArray(parsed)) {
            return parsed
                .map(tag => typeof tag === 'string' ? tag.trim() : '')
                .filter(tag => tag.length > 0);
        }
    } catch (error) {
        // Fallback auf alte/kommagetrennte Werte.
    }

    return normalizedValue
        .split(',')
        .map(tag => tag.trim())
        .filter(tag => tag.length > 0);
}

function initTagInput() {
    const tagsInput = document.getElementById('tagsInput');
    const tagInputField = document.getElementById('tagInputField');
    const tagChipsContainer = document.getElementById('tagChipsContainer');

    if (!tagInputField || !tagChipsContainer || !tagsInput) return;

    // Parse initial tags aus dem hidden input feld
    const initialTags = parseTagsField(tagsInput.value);

    let currentTags = [];

    function renderTags() {
        tagChipsContainer.innerHTML = '';
        currentTags.forEach(function(tag, index) {
            const chip = document.createElement('div');
            chip.className = 'tag-chip';
            chip.innerHTML = `${escapeHtml(tag)}<span class="tag-chip-remove" title="Entfernen">×</span>`;

            chip.addEventListener('click', function() {
                removeTag(index);
            });

            tagChipsContainer.appendChild(chip);
        });

        // Update hidden input field
        tagsInput.value = JSON.stringify(currentTags);
    }

    function addTag(tag) {
        const trimmed = (tag || '').trim().toLowerCase();

        if (!trimmed) return;

        if (currentTags.some(function(t) { return t.toLowerCase() === trimmed; })) {
            showError('Dieses Tag existiert bereits.');
            return;
        }

        if (trimmed.length > 20) {
            showError('Tag zu lang (maximal 20 Zeichen).');
            return;
        }

        currentTags.push(trimmed);
        renderTags();
        return true;
    }

    function addTagsFromInput(value) {
        const tagsToAdd = parseTagsField(value);
        let addedAny = false;

        tagsToAdd.forEach(function(tag) {
            if (addTag(tag)) {
                addedAny = true;
            }
        });

        if (addedAny) {
            tagInputField.value = '';
            tagInputField.focus();
        }
    }

    function removeTag(index) {
        currentTags.splice(index, 1);
        renderTags();
        tagInputField.focus();
    }

    // Enter-Taste oder "+" Button
    tagInputField.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            addTagsFromInput(this.value);
        }

        if (e.key === 'Backspace' && this.value === '' && currentTags.length > 0) {
            removeTag(currentTags.length - 1);
        }
    });

    // Ein Komma erlaubt das schnelle Hinzufügen mehrerer Tags, auch beim Einfügen von Listen.
    tagInputField.addEventListener('keyup', function() {
        if (this.value.includes(',')) {
            addTagsFromInput(this.value);
        }
    });

    // Initial rendering
    currentTags = [...initialTags];
    renderTags();
}
