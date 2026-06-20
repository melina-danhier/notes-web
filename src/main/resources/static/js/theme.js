(function () {
    const storageKey = 'notes-theme';
    const root = document.documentElement;

    function getSavedTheme() {
        try {
            return localStorage.getItem(storageKey);
        } catch (error) {
            return null;
        }
    }

    function applyTheme(theme) {
        root.dataset.bsTheme = theme;
        root.dataset.theme = theme;
        root.style.colorScheme = theme;

        document.querySelectorAll('[data-theme-toggle]').forEach(function (button) {
            const dark = theme === 'dark';
            button.setAttribute('aria-label', dark ? 'Hellen Modus aktivieren' : 'Dunklen Modus aktivieren');
            button.setAttribute('title', dark ? 'Heller Modus' : 'Dunkler Modus');
            button.setAttribute('aria-pressed', String(dark));
            const label = button.querySelector('[data-theme-label]');
            if (label) label.textContent = dark ? 'Light Mode' : 'Dark Mode';
            const icon = button.querySelector('[data-theme-icon]');
            if (icon) icon.textContent = dark ? '☀' : '☾';
        });
    }

    function currentTheme() {
        return getSavedTheme() === 'dark' ? 'dark' : 'light';
    }

    applyTheme(currentTheme());

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-theme-toggle]').forEach(function (button) {
            button.addEventListener('click', function () {
                const nextTheme = root.dataset.theme === 'dark' ? 'light' : 'dark';
                try {
                    localStorage.setItem(storageKey, nextTheme);
                } catch (error) {
                    // The selected theme still applies for the current page.
                }
                applyTheme(nextTheme);
            });
        });
        applyTheme(currentTheme());
    });
})();
