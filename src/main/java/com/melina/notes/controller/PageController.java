package com.melina.notes.controller;

import com.melina.notes.dto.EditNoteDTO;
import com.melina.notes.dto.NoteDTO;
import com.melina.notes.dto.TagDTO;
import com.melina.notes.service.NoteService;
import com.melina.notes.security.CustomUserDetails;
import com.melina.notes.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PageController {
    private final NoteService noteService;
    private final TagService tagService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @GetMapping("/")
    public String homeToLogin() {
        return "login";
    }

    @GetMapping("/notes")
    public String notesPage(Model model,
                            @AuthenticationPrincipal CustomUserDetails user,
                            @RequestParam(defaultValue = "0") int pageNo,
                            @RequestParam(defaultValue = "9") int pageSize) {
        model.addAttribute("noteDto", new EditNoteDTO());
        Page<NoteDTO> notes = noteService.getAllNotesByUserId(user.getId(), PageRequest.of(pageNo, pageSize));
        model.addAttribute("notes", notes);
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", notes.getTotalPages());
        model.addAttribute("displayname", user.getDisplayName());
        List<TagDTO> tags = tagService.getAllTagsByUserId(user.getId());
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