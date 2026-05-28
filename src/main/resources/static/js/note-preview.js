'use strict';

/**
 * Markdown-Rendering und Note-Preview Modal Logic
 */

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

    document.getElementById('modalTitle').textContent = note.title || '(Untitled)';

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
        contentContainer.innerHTML = renderMarkdownPreview(note.content || '');
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

export function editFromModal() {
    if (currentEditNoteId) {
        window.location.href = `/notes/${currentEditNoteId}`;
    }
}

function renderMarkdownPreview(markdown) {
    const normalized = (markdown || '').replace(/\r\n/g, '\n');

    if (!normalized.trim()) {
        return '<div class="markdown-empty-state text-muted">Keine Inhalte.</div>';
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
    let output = String(text ?? '');
    const codeSegments = [];
    const linkSegments = [];

    output = output.replace(/\[([^\]\n]+)]\(([^)\s]+)(?:\s+"([^"]+)")?\)/g, function(match, label, url, title) {
        const safeUrl = sanitizeMarkdownUrl(url);
        if (!safeUrl) {
            return match;
        }

        const token = `@@LINK_${linkSegments.length}@@`;
        const titleAttribute = title ? ` title="${escapeHtml(title)}"` : '';
        linkSegments.push(`<a href="${escapeHtml(safeUrl)}"${titleAttribute} target="_blank" rel="noopener noreferrer">${escapeHtml(label)}</a>`);
        return token;
    });

    output = escapeHtml(output);

    output = output.replace(/`([^`\n]+)`/g, function(match, code) {
        const token = `@@CODE_${codeSegments.length}@@`;
        codeSegments.push(`<code>${escapeHtml(code)}</code>`);
        return token;
    });

    output = output.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    output = output.replace(/(^|[^*])\*([^*\n]+)\*(?!\*)/g, '$1<em>$2</em>');
    output = output.replace(/__([^_]+)__/g, '<strong>$1</strong>');
    output = output.replace(/(^|[^_])_([^_\n]+)_(?!_)/g, '$1<em>$2</em>');

    output = output.replace(/@@CODE_(\d+)@@/g, function(match, index) {
        return codeSegments[Number(index)] || '';
    });

    output = output.replace(/@@LINK_(\d+)@@/g, function(match, index) {
        return linkSegments[Number(index)] || '';
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

    if (/^(?:www\.|localhost|[a-z0-9-]+(?:\.[a-z0-9-]+)+)(?::\d+)?(?:[/?#]|$)/i.test(trimmed)) {
        return `https://${trimmed}`;
    }

    return null;
}

export function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}


