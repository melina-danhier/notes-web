"use strict";

// Zentralisiertes Markdown-Rendering: exportiert eine kompakte, sichere Vorschau-Funktion
export function renderMarkdownPreview(markdown) {
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

        const headingMatch = trimmed.match(/^(#{1,6})\s+(.*)$/);
        if (headingMatch) { blocks.push(`<h${headingMatch[1].length}>${renderMarkdownInline(headingMatch[2])}</h${headingMatch[1].length}>`); index++; continue; }

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
    return trimmed.startsWith('```') || /^#{1,6}\s+/.test(trimmed) || /^>\s?/.test(trimmed) || /^\s*(?:[-*+]\s+|\d+\.\s+)/.test(line);
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

    output = output.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    output = output.replace(/(^|[^*])\*([^*\n]+)\*(?!\*)/g, '$1<em>$2</em>');
    output = output.replace(/__([^_]+)__/g, '<strong>$1</strong>');
    output = output.replace(/(^|[^_])_([^_\n]+)_(?!_)/g, '$1<em>$2</em>');

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

