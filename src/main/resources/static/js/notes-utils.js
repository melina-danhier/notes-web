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