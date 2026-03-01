/**
 * Toggle Passwort Sichtbarkeit
 * Findet das Passwort-Feld basierend auf dem Kontext
 */
function togglePassword() {
    // Wir suchen das Input-Feld, das direkt vor dem geklickten Icon liegt
    // Da wir in verschiedenen Forms sind, nehmen wir das nächste input-Feld
    const icon = event.target;
    const input = icon.previousElementSibling; // Das Eingabefeld vor dem Icon

    if (input.type === "password") {
        input.type = "text";
        icon.classList.remove("fa-eye");
        icon.classList.add("fa-eye-slash");
    } else {
        input.type = "password";
        icon.classList.remove("fa-eye-slash");
        icon.classList.add("fa-eye");
    }
}

/**
 * Formularevents
 */
