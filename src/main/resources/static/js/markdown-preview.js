'use strict';

import { marked } from 'https://cdn.jsdelivr.net/npm/marked@9.1.6/+esm';

// Configure marked for compact preview rendering
marked.setOptions({
    breaks: true,
    gfm: true,
});

/**
 * Renderiert Markdown-Previews auf Note-Karten
 */
export function renderMarkdownPreviews() {
    document.querySelectorAll('.card-preview-markdown').forEach(el => {
        const markdown = el.getAttribute('data-markdown');
        if (markdown) {
            try {
                const html = marked.parse(markdown);
                el.innerHTML = html;
                // Entferne leere <p>-Tags und trimme
                el.querySelectorAll('p:empty').forEach(p => p.remove());
            } catch (e) {
                console.error('Error parsing markdown:', e);
                el.textContent = markdown;
            }
        }
    });
}
