'use strict';

export function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
}

export function getCsrfHeader() {
    return document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
}

export function updateLocalTimes() {
    document.querySelectorAll('.local-time span').forEach(timeSpan => {
        const serverText = timeSpan.textContent.trim();
        if (!serverText.match(/^\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}$/)) return;

        const [datePart, timePart] = serverText.split(' ');
        const [day, month, year] = datePart.split('.').map(Number);
        const [hour, minute] = timePart.split(':').map(Number);

        const utcDate = new Date(Date.UTC(year, month - 1, day, hour, minute));
        timeSpan.textContent = utcDate.toLocaleDateString('de-DE', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit', hour12: false
        });
    });
}

export function showLoading(noteId) {
    const btn = document.querySelector(`#note-${noteId} button[title="Löschen"]`);
    if (btn) {
        btn.innerHTML = '⏳';
        btn.disabled = true;
    }
}

export function hideLoading(noteId) {
    const btn = document.querySelector(`#note-${noteId} button[title="Löschen"]`);
    if (btn) {
        btn.innerHTML = '🗑️';
        btn.disabled = false;
    }
}

export function highlightText(text, searchTerm) {
    if (!searchTerm || !text) return escapeHtml(text || '');
    
    const regex = new RegExp(`(${searchTerm.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
    return escapeHtml(text).replace(regex, '<mark>$1</mark>');
}

export function highlightMarkdownContent(html, searchTerm) {
    if (!searchTerm || !html) return html;

    const template = document.createElement('template');
    template.innerHTML = html;
    highlightTextNodes(template.content, searchTerm);
    return template.innerHTML;
}

function highlightTextNodes(root, searchTerm) {
    const regex = new RegExp(`(${escapeRegExp(searchTerm)})`, 'gi');
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    const textNodes = [];

    while (walker.nextNode()) {
        textNodes.push(walker.currentNode);
    }

    textNodes.forEach(textNode => {
        regex.lastIndex = 0;
        if (!regex.test(textNode.nodeValue)) {
            return;
        }

        regex.lastIndex = 0;
        const fragment = document.createDocumentFragment();
        let lastIndex = 0;

        textNode.nodeValue.replace(regex, (match, _group, offset) => {
            fragment.append(document.createTextNode(textNode.nodeValue.slice(lastIndex, offset)));

            const mark = document.createElement('mark');
            mark.textContent = match;
            fragment.append(mark);

            lastIndex = offset + match.length;
            return match;
        });

        fragment.append(document.createTextNode(textNode.nodeValue.slice(lastIndex)));
        textNode.replaceWith(fragment);
    });
}

function escapeRegExp(value) {
    return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

export function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}
