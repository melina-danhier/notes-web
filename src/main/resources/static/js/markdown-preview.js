'use strict';

import { renderMarkdownPreview } from './markdown.js';

export function renderMarkdownPreviews() {
    document.querySelectorAll('.card-preview-markdown').forEach(preview => {
        preview.innerHTML = renderMarkdownPreview(preview.dataset.markdown || '');
    });
}
