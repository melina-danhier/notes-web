'use strict';

import { renderMarkdownPreview } from './markdown.js';

/**
 * Renderiert Markdown-Previews auf Note-Karten
 */
export function renderMarkdownPreviews() {
    document.querySelectorAll('.card-preview-markdown').forEach(el => {
        const markdown = el.getAttribute('data-markdown');
        if (markdown) {
            try {
                el.innerHTML = renderMarkdownPreview(markdown);
            } catch (e) {
                console.error('Error parsing markdown:', e);
                el.textContent = markdown;
            }
        }
    });
}
