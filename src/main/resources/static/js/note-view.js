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

    document.getElementById('titleInput')?.focus();

    initTooltips();
    initSaveButton();
    initAutoResize();
    initMarkdownPreview();
    initMarkdownToolbar();
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

    const resize = function() {
        this.style.height = 'auto';
        this.style.height = `${this.scrollHeight}px`;
    };

    textarea.style.overflow = 'hidden';
    textarea.style.resize = 'none';
    textarea.addEventListener('input', resize);
    window.addEventListener('resize', () => resize.call(textarea));
    resize.call(textarea);
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

    toolbar.addEventListener('click', function(event) {
        const button = event.target.closest('button[data-md-action]');
        if (!button) return;

        event.preventDefault();
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
        case 'bold':
            insertMarkdownWrap(textarea, '**', '**', 'fett');
            break;
        case 'italic':
            insertMarkdownWrap(textarea, '*', '*', 'kursiv');
            break;
        case 'inline-code':
            insertMarkdownWrap(textarea, '`', '`', 'code');
            break;
        case 'code-block':
            insertMarkdownBlock(textarea, '```\n', '\n```', 'Code hier');
            break;
        case 'link':
            insertMarkdownLink(textarea);
            break;
        case 'quote':
            prefixMarkdownLines(textarea, '> ');
            break;
        case 'bullet-list':
            prefixMarkdownLines(textarea, '- ');
            break;
        case 'ordered-list':
            prefixMarkdownLines(textarea, '1. ', { ordered: true });
            break;
        default:
            console.warn(`Unbekannte Markdown-Aktion: ${action}`);
    }
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

function insertMarkdownBlock(textarea, before, after, placeholder) {
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

function renderMarkdownPreview(markdown) {
    const normalized = (markdown || '').replace(/\r\n/g, '\n');

    if (!normalized.trim()) {
        return '<div class="markdown-empty-state text-muted">Hier erscheint die Vorschau deiner Notiz.</div>';
    }

    return renderMarkdownBlocks(normalized);
}

function renderMarkdownBlocks(markdown) {
    const lines = markdown.split('\n');
    const blocks = [];
    let index = 0;

    while (index < lines.length) {
        const line = lines[index];

        if (line.trim() === '') {
            index += 1;
            continue;
        }

        const trimmed = line.trim();

        if (trimmed.startsWith('```')) {
            const language = trimmed.slice(3).trim();
            index += 1;

            const codeLines = [];
            while (index < lines.length && !lines[index].trim().startsWith('```')) {
                codeLines.push(lines[index]);
                index += 1;
            }

            if (index < lines.length) {
                index += 1;
            }

            const languageClass = language ? ` class="language-${escapeHtml(language)}"` : '';
            blocks.push(`<pre><code${languageClass}>${escapeHtml(codeLines.join('\n'))}</code></pre>`);
            continue;
        }

        const headingMatch = trimmed.match(/^(#{1,6})\s+(.*)$/);
        if (headingMatch) {
            const level = headingMatch[1].length;
            blocks.push(`<h${level}>${renderMarkdownInline(headingMatch[2])}</h${level}>`);
            index += 1;
            continue;
        }

        if (/^>\s?/.test(trimmed)) {
            const quoteLines = [];
            while (index < lines.length && /^>\s?/.test(lines[index].trim())) {
                quoteLines.push(lines[index].replace(/^\s*>\s?/, ''));
                index += 1;
            }

            const quoteHtml = quoteLines.map(renderMarkdownInline).join('<br>');
            blocks.push(`<blockquote><p class="mb-0">${quoteHtml}</p></blockquote>`);
            continue;
        }

        if (/^\s*(?:[-*+]\s+|\d+\.\s+)/.test(line)) {
            const ordered = /^\s*\d+\.\s+/.test(line);
            const items = [];

            while (index < lines.length && /^\s*(?:[-*+]\s+|\d+\.\s+)/.test(lines[index])) {
                const current = lines[index];
                if (ordered !== /^\s*\d+\.\s+/.test(current)) {
                    break;
                }

                items.push(current.replace(/^\s*(?:[-*+]\s+|\d+\.\s+)/, ''));
                index += 1;
            }

            const tagName = ordered ? 'ol' : 'ul';
            const listItems = items.map(item => `<li>${renderMarkdownInline(item)}</li>`).join('');
            blocks.push(`<${tagName}>${listItems}</${tagName}>`);
            continue;
        }

        const paragraphLines = [];
        while (index < lines.length && lines[index].trim() !== '' && !isMarkdownBlockBoundary(lines[index])) {
            paragraphLines.push(lines[index]);
            index += 1;
        }

        const paragraphHtml = paragraphLines.map(renderMarkdownInline).join('<br>');
        blocks.push(`<p>${paragraphHtml}</p>`);
    }

    return blocks.join('');
}

function isMarkdownBlockBoundary(line) {
    const trimmed = line.trim();

    return trimmed.startsWith('```') ||
        /^#{1,6}\s+/.test(trimmed) ||
        /^>\s?/.test(trimmed) ||
        /^\s*(?:[-*+]\s+|\d+\.\s+)/.test(line);
}

function renderMarkdownInline(text) {
    let output = escapeHtml(text);
    const codeSegments = [];

    output = output.replace(/`([^`\n]+)`/g, function(match, code) {
        const token = `@@CODE_${codeSegments.length}@@`;
        codeSegments.push(`<code>${escapeHtml(code)}</code>`);
        return token;
    });

    output = output.replace(/\[([^]]+)]\(([^)\s]+)(?:\s+"([^"]+)")?\)/g, function(match, label, url, title) {
        const safeUrl = sanitizeMarkdownUrl(url);
        if (!safeUrl) {
            return label;
        }

        const titleAttribute = title ? ` title="${escapeHtml(title)}"` : '';
        return `<a href="${escapeHtml(safeUrl)}"${titleAttribute} target="_blank" rel="noopener noreferrer">${label}</a>`;
    });

    output = output.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    output = output.replace(/(^|[^*])\*([^*\n]+)\*(?!\*)/g, '$1<em>$2</em>');
    output = output.replace(/__([^_]+)__/g, '<strong>$1</strong>');
    output = output.replace(/(^|[^_])_([^_\n]+)_(?!_)/g, '$1<em>$2</em>');

    output = output.replace(/@@CODE_(\d+)@@/g, function(match, index) {
        return codeSegments[Number(index)] || '';
    });

    return output;
}

function sanitizeMarkdownUrl(url) {
    const trimmed = String(url || '').trim();

    if (!trimmed) {
        return null;
    }

    if (/^(https?:|mailto:|tel:)/i.test(trimmed) || /^[/.#?]/.test(trimmed)) {
        return trimmed;
    }

    return null;
}

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
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
    toastDiv.setAttribute('aria-live', 'assertive');
    toastDiv.setAttribute('aria-atomic', 'true');

    const toastBodyWrapper = document.createElement('div');
    toastBodyWrapper.className = 'd-flex';

    const toastBody = document.createElement('div');
    toastBody.className = 'toast-body';
    toastBody.textContent = msg;

    const closeButton = document.createElement('button');
    closeButton.type = 'button';
    closeButton.className = 'btn-close btn-close-white me-2 m-auto';
    closeButton.setAttribute('data-bs-dismiss', 'toast');
    closeButton.setAttribute('aria-label', 'Schließen');

    toastBodyWrapper.appendChild(toastBody);
    toastBodyWrapper.appendChild(closeButton);
    toastDiv.appendChild(toastBodyWrapper);
    toastContainer.appendChild(toastDiv);

    const toast = new bootstrap.Toast(toastDiv);
    toast.show();

    setTimeout(() => {
        if (toastDiv.parentNode) {
            toastDiv.remove();
        }
    }, 4000);
}

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

    splitDivider.addEventListener('pointerdown', function(event) {
        if (previewContainer.style.display === 'none') return;

        isDragging = true;
        splitDivider.setPointerCapture(event.pointerId);
        document.body.classList.add('split-resizing');
        event.preventDefault();
    });

    document.addEventListener('pointermove', function(event) {
        if (!isDragging) return;

        const rect = splitRoot.getBoundingClientRect();
        const dividerWidth = parseFloat(getComputedStyle(splitRoot).getPropertyValue('--split-divider-width')) || 12;
        const availableWidth = Math.max(rect.width - dividerWidth, 1);
        const leftWidth = event.clientX - rect.left;
        const ratio = clamp(leftWidth / availableWidth, minRatio, maxRatio);

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

function initTagInput() {
    const tagsInput = document.getElementById('tagsInput');
    const tagInputField = document.getElementById('tagInputField');
    const tagChipsContainer = document.getElementById('tagChipsContainer');

    if (!tagInputField || !tagChipsContainer || !tagsInput) return;

    // Parse initial tags aus dem hidden input feld
    const initialTags = tagsInput.value
        ? tagsInput.value.split(',').map(t => t.trim()).filter(t => t.length > 0)
        : [];

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
        tagsInput.value = currentTags.join(', ');
    }

    function addTag(tag) {
        const trimmed = (tag || '').trim().toLowerCase();

        if (!trimmed) return;

        if (currentTags.some(function(t) { return t.toLowerCase() === trimmed; })) {
            showError('Dieses Tag existiert bereits.');
            return;
        }

        if (trimmed.length > 50) {
            showError('Tag zu lang (maximal 50 Zeichen).');
            return;
        }

        currentTags.push(trimmed);
        renderTags();
        tagInputField.value = '';
        tagInputField.focus();
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
            addTag(this.value);
        }

        if (e.key === 'Backspace' && this.value === '' && currentTags.length > 0) {
            removeTag(currentTags.length - 1);
        }
    });

    // "+" Button simulation via komma
    tagInputField.addEventListener('keyup', function() {
        if (this.value.includes(',')) {
            const tag = this.value.replace(/,/g, '');
            addTag(tag);
        }
    });

    // Initial rendering
    currentTags = [...initialTags];
    renderTags();
}

// ======================== TOOLTIPS ========================

function initTooltips() {
    // Bootstrap Tooltips aktivieren
    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function(element) {
        new bootstrap.Tooltip(element);
    });
}
