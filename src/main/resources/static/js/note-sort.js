'use strict';

export function initSorting() {
    const menu = document.getElementById('sortMenu');
    if (!menu) return;

    const labels = {created: 'Erstellt', updated: 'Geändert', title: 'Titel'};

    function getLabel(field, activeDir) {
        const oppositeArrow = activeDir === 'asc' ? '↓' : '↑';
        return `${labels[field] ?? field} ${oppositeArrow}`;
    }

    menu.querySelectorAll('.dropdown-item').forEach(item => {
        item.onclick = e => {
            e.preventDefault();

            const isActive = item.classList.contains('active');
            const currentDir = item.dataset.sortDir;
            const newDir = isActive
                ? (currentDir === 'asc' ? 'desc' : 'asc')
                : currentDir;

            menu.querySelectorAll('.dropdown-item')
                .forEach(i => i.classList.remove('active'));
            item.classList.add('active');

            item.dataset.sortDir = newDir;
            const activeArrow = newDir === 'asc' ? '↑' : '↓';
            document.getElementById('selectedSortDisplay').textContent =
                `${labels[item.dataset.sortField] ?? item.dataset.sortField} ${activeArrow}`;

            item.textContent = getLabel(item.dataset.sortField, newDir);

            applySort(item.dataset.sortField, newDir);
        };
    });

    const params = new URLSearchParams(window.location.search);
    const activeField = params.get('sortField');
    const activeDir = params.get('sortDir');

    if (activeField && activeDir) {
        menu.querySelectorAll('.dropdown-item').forEach(i => i.classList.remove('active'));
        const match = [...menu.querySelectorAll('.dropdown-item')]
            .find(i => i.dataset.sortField === activeField);
        if (match) {
            match.dataset.sortDir = activeDir;
            const activeArrow = activeDir === 'asc' ? '↑' : '↓';
            match.classList.add('active');
            document.getElementById('selectedSortDisplay').textContent =
                `${labels[activeField] ?? activeField} ${activeArrow}`;
            match.textContent = getLabel(activeField, activeDir);
        }
    } else {
        const defaultActive = menu.querySelector('.dropdown-item.active');
        if (defaultActive) {
            const field = defaultActive.dataset.sortField;
            const dir = defaultActive.dataset.sortDir;
            const activeArrow = dir === 'asc' ? '↑' : '↓';
            document.getElementById('selectedSortDisplay').textContent =
                `${labels[field] ?? field} ${activeArrow}`;
            defaultActive.textContent = getLabel(field, dir);
        }
    }
}

function applySort(field, dir) {
    const url = new URL(window.location.href);
    url.searchParams.set('sortField', field);
    url.searchParams.set('sortDir', dir);
    url.searchParams.set('pageNo', '0');
    window.location.href = url.toString();
}