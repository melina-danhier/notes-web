'use strict';

// 🎯 ALLE MODULES IMPORTIEREN
import { initNotesApp } from './notes-events.js';
import './notes-delete.js';  // Globale deleteNote() Funktion
import './notes-filter.js';  // Filter initialisieren

// 🚀 APP STARTEN
document.addEventListener('DOMContentLoaded', initNotesApp);