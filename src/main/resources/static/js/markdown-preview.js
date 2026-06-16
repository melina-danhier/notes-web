'use strict';

import { renderMarkdownPreview } from './markdown.js';

export function renderMarkdownPreviews() {
    document.querySelectorAll('.card-preview-markdown, [data-markdown]').forEach(function(previewElement) {
        const markdown = previewElement.dataset.markdown || '';
        try {
            previewElement.innerHTML = renderMarkdownPreview(markdown);
        } catch (error) {
            console.error('Error parsing markdown:', error);
            previewElement.textContent = markdown;
        }
    });
}
