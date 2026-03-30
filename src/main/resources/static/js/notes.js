'use strict';

import { initNotesApp } from './notes-init.js';
import './notes-delete.js';
import { initSearchAndFilter } from './notes-filter.js';
import { initSorting } from './note-sort.js';

document.addEventListener('DOMContentLoaded', () => {
    initNotesApp()
    initSearchAndFilter();
    initSorting();
});