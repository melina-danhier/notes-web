"use strict";

// Zentralisiertes Markdown-Rendering: exportiert eine kompakte, sichere Vorschau-Funktion
export function renderMarkdownPreview(markdown, options = {}) {
    const normalized = (markdown || '').replace(/\r\n/g, '\n');
    if (!normalized.trim()) {
        return '<div class="markdown-empty-state text-muted">Hier erscheint die Vorschau deiner Notiz.</div>';
    }
    return renderMarkdownBlocks(normalized, {
        interactiveTasks: options.interactiveTasks === true,
        taskIndex: 0
    });
}

function renderMarkdownBlocks(markdown, context) {
    const lines = markdown.split('\n');
    const blocks = [];
    let index = 0;

    while (index < lines.length) {
        const line = lines[index];
        if (line.trim() === '') { index++; continue; }
        const trimmed = line.trim();

        if (trimmed.startsWith('```')) {
            const language = trimmed.slice(3).trim();
            index++;
            const codeLines = [];
            while (index < lines.length && !lines[index].trim().startsWith('```')) {
                codeLines.push(lines[index]); index++;
            }
            if (index < lines.length) index++;
            const languageClass = language ? ` class="language-${escapeHtml(language)}"` : '';
            blocks.push(`<pre><code${languageClass}>${escapeHtml(codeLines.join('\n'))}</code></pre>`);
            continue;
        }

        const toggleDirective = parseToggleDirective(trimmed);
        if (toggleDirective !== null) {
            index++;
            const toggleLines = [];
            while (index < lines.length && !isToggleDirectiveEnd(lines[index].trim())) {
                toggleLines.push(lines[index]);
                index++;
            }
            if (index < lines.length) index++;

            const content = renderMarkdownBlocks(toggleLines.join('\n'), context);
            blocks.push(
                `<details class="markdown-toggle" open>` +
                `<summary>${renderMarkdownInline(toggleDirective)}</summary>` +
                `<div class="markdown-toggle-content">${content}</div>` +
                `</details>`
            );
            continue;
        }

        if (isToggleDirectiveEnd(trimmed)) {
            index++;
            continue;
        }

        const details = parseDetailsStart(trimmed);
        if (details !== null) {
            index++;
            let summary = 'Abschnitt';
            const summaryMatch = index < lines.length
                ? lines[index].trim().match(/^<summary>(.*?)<\/summary>$/i)
                : null;
            if (summaryMatch) {
                summary = summaryMatch[1];
                index++;
            }

            const detailsLines = [];
            while (index < lines.length && !/^<\/details>$/i.test(lines[index].trim())) {
                detailsLines.push(lines[index]);
                index++;
            }
            if (index < lines.length) index++;

            const content = renderMarkdownBlocks(detailsLines.join('\n'), context);
            blocks.push(
                `<details class="markdown-toggle"${details.open ? ' open' : ''}>` +
                `<summary>${renderMarkdownInline(summary)}</summary>` +
                `<div class="markdown-toggle-content">${content}</div>` +
                `</details>`
            );
            continue;
        }

        const toggleHeading = parseToggleHeading(trimmed);
        if (toggleHeading !== null) {
            index++;
            const toggleLines = [];
            while (index < lines.length
                && parseToggleHeading(lines[index].trim()) === null
                && !isToggleEnd(lines[index].trim())) {
                toggleLines.push(lines[index]);
                index++;
            }
            if (index < lines.length && isToggleEnd(lines[index].trim())) {
                index++;
            }
            const content = renderMarkdownBlocks(toggleLines.join('\n'), context);
            blocks.push(
                `<details class="markdown-toggle" open>` +
                `<summary>${renderMarkdownInline(toggleHeading)}</summary>` +
                `<div class="markdown-toggle-content">${content}</div>` +
                `</details>`
            );
            continue;
        }

        if (isToggleEnd(trimmed)) {
            index++;
            continue;
        }

        const task = parseTask(trimmed);
        if (task !== null) {
            const taskIndex = context.taskIndex++;
            const disabled = context.interactiveTasks ? '' : ' disabled';
            const checked = task.checked ? ' checked' : '';
            blocks.push(
                `<label class="markdown-task${task.checked ? ' is-complete' : ''}">` +
                `<input type="checkbox" data-task-index="${taskIndex}"${checked}${disabled}>` +
                `<span>${renderMarkdownInline(task.label)}</span>` +
                `</label>`
            );
            index++;
            continue;
        }

        const headingMatch = trimmed.match(/^(#{1,6})\s+(.*)$/);
        if (headingMatch) { blocks.push(`<h${headingMatch[1].length}>${renderMarkdownInline(headingMatch[2])}</h${headingMatch[1].length}>`); index++; continue; }

        if (/^(?:-{3,}|\*{3,}|_{3,})$/.test(trimmed)) {
            blocks.push('<hr>');
            index++;
            continue;
        }

        if (/^>\s?/.test(trimmed)) {
            const quoteLines = [];
            while (index < lines.length && /^>\s?/.test(lines[index].trim())) { quoteLines.push(lines[index].replace(/^\s*>\s?/, '')); index++; }
            blocks.push(`<blockquote><p class="mb-0">${quoteLines.map(renderMarkdownInline).join('<br>')}</p></blockquote>`);
            continue;
        }

        if (/^\s*(?:[-*+]\s+|\d+\.\s+)/.test(line)) {
            const ordered = /^\s*\d+\.\s+/.test(line);
            const items = [];
            while (index < lines.length && /^\s*(?:[-*+]\s+|\d+\.\s+)/.test(lines[index])) {
                const current = lines[index];
                if (ordered !== /^\s*\d+\.\s+/.test(current)) break;
                items.push(current.replace(/^\s*(?:[-*+]\s+|\d+\.\s+)/, ''));
                index++;
            }
            const tagName = ordered ? 'ol' : 'ul';
            blocks.push(`<${tagName}>${items.map(i => `<li>${renderMarkdownInline(i)}</li>`).join('')}</${tagName}>`);
            continue;
        }

        const paragraphLines = [];
        while (index < lines.length && lines[index].trim() !== '' && !isMarkdownBlockBoundary(lines[index])) { paragraphLines.push(lines[index]); index++; }
        blocks.push(`<p>${paragraphLines.map(renderMarkdownInline).join('<br>')}</p>`);
    }

    return blocks.join('');
}

function isMarkdownBlockBoundary(line) {
    const trimmed = line.trim();
    return trimmed.startsWith('```')
        || parseToggleDirective(trimmed) !== null
        || isToggleDirectiveEnd(trimmed)
        || parseDetailsStart(trimmed) !== null
        || parseToggleHeading(trimmed) !== null
        || isToggleEnd(trimmed)
        || parseTask(trimmed) !== null
        || /^#{1,6}\s+/.test(trimmed)
        || /^(?:-{3,}|\*{3,}|_{3,})$/.test(trimmed)
        || /^>\s?/.test(trimmed)
        || /^\s*(?:[-*+]\s+|\d+\.\s+)/.test(line);
}

function parseToggleDirective(line) {
    const match = line.match(/^:::toggle\s+(.+?)\s*$/i);
    return match ? match[1] : null;
}

function isToggleDirectiveEnd(line) {
    return /^:::\s*$/.test(line);
}

function parseDetailsStart(line) {
    const match = line.match(/^<details(?:\s+(open))?\s*>$/i);
    return match ? { open: Boolean(match[1]) } : null;
}

function parseToggleHeading(line) {
    // Legacy-Unterstützung für bereits gespeicherte Bereiche. Neue Bereiche
    // verwenden standardnahes <details>/<summary>-HTML.
    const match = line.match(/^>\s+(.+?)\s*;\s*$/);
    return match ? match[1] : null;
}

function isToggleEnd(line) {
    return /^>\s*;\s*$/.test(line);
}

function parseTask(line) {
    const match = line.match(/^([-*+]\s+)?\[([ xX])]\s+(.+?)\s*$/);
    if (!match) return null;
    return {
        checked: match[2].toLowerCase() === 'x',
        label: match[1] ? match[3] : match[3].replace(/;\s*$/, '')
    };
}

function renderMarkdownInline(text) {
    let output = String(text ?? '');
    const codeSegments = [];
    const linkSegments = [];

    output = output.replace(/\[([^\]\n]+)]\(([^)\s]+)(?:\s+"([^"]+)")?\)/g, function(match, label, url, title) {
        const safeUrl = sanitizeMarkdownUrl(url);
        if (!safeUrl) return match;
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

    output = output.replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>');
    output = output.replace(/__([^_\n]+)__/g, '<strong>$1</strong>');
    output = output.replace(/(^|[^*])\*([^*\n]+)\*(?!\*)/g, '$1<em>$2</em>');
    output = output.replace(/(^|[^_])_([^_\n]+)_(?!_)/g, '$1<em>$2</em>');
    output = output.replace(/~~([^~\n]+)~~/g, '<del>$1</del>');

    output = output.replace(/@@CODE_(\d+)@@/g, function(_, idx) { return codeSegments[Number(idx)] || ''; });
    output = output.replace(/@@LINK_(\d+)@@/g, function(_, idx) { return linkSegments[Number(idx)] || ''; });

    return output;
}

function sanitizeMarkdownUrl(url) {
    const trimmed = String(url || '').trim();
    if (!trimmed) return null;
    if (/^(https?:|mailto:|tel:)/i.test(trimmed) || /^[/.#?]/.test(trimmed)) return trimmed;
    if (/^(?:www\.|localhost|[a-z0-9-]+(?:\.[a-z0-9-]+)+)(?::\d+)?(?:[/?#]|$)/i.test(trimmed)) return `https://${trimmed}`;
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

