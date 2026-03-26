package com.melina.notes.controller;

import com.melina.notes.dto.EditNoteDTO;
import com.melina.notes.dto.NoteDTO;
import com.melina.notes.dto.TagDTO;
import com.melina.notes.service.NoteService;
import com.melina.notes.security.CustomUserDetails;
import com.melina.notes.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PageController {
    private final NoteService noteService;
    private final TagService tagService;

    @GetMapping("/notes")
    public String notesPage(Model model, @AuthenticationPrincipal CustomUserDetails user) {
        model.addAttribute("noteDto", new EditNoteDTO());
        List<NoteDTO> notes = noteService.getAllNotesByUserId(user.getId());
        model.addAttribute("notes", notes);
        model.addAttribute("displayname", user.getDisplayName());
        List<TagDTO> tags = tagService.getAllTags();
        model.addAttribute("allTags", tags);
        return "notes";
    }

    @GetMapping("/notes/new")
    public String newNote(Model model) {
        model.addAttribute("noteDto", new EditNoteDTO());
        return "note-view";
    }

    @GetMapping("/notes/{id}")
    public String editNote(@PathVariable Long id, Model model, @AuthenticationPrincipal CustomUserDetails user) {
        EditNoteDTO note = noteService.getNoteByIdForView(user.getId(), id);
        model.addAttribute("noteDto", note);
        return "note-view";
    }

}