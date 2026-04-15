package com.melina.notes.controller;

import com.melina.notes.dto.EditNoteDTO;
import com.melina.notes.dto.NoteDTO;
import com.melina.notes.dto.TagDTO;
import com.melina.notes.filter.NoteFilter;
import com.melina.notes.service.NoteService;
import com.melina.notes.security.CustomUserDetails;
import com.melina.notes.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
                            @RequestParam(required = false, defaultValue = "0") int pageNo,
                            @RequestParam(required = false, defaultValue = "9") int pageSize,
                            @RequestParam(required = false, defaultValue = "created") String sortField,
                            @RequestParam(required = false, defaultValue = "desc") String sortDir,
                            @RequestParam(required = false) String tagFilter,
                            @RequestParam(required = false) String search) {

        NoteFilter filter = new NoteFilter();
        filter.setUserId(user.getId());
        filter.setTagFilter(tagFilter);
        filter.setSearchTerm(search);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortField);
        Page<NoteDTO> notes = noteService.getAllNotes(filter, PageRequest.of(pageNo, pageSize, sort));

        model.addAttribute("notes", notes);
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", notes.getTotalPages());
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("tagFilter", tagFilter);
        model.addAttribute("search", search);

        List<TagDTO> tags = tagService.getAllTagsByUserId(user.getId());
        model.addAttribute("allTags", tags);
        model.addAttribute("displayname", user.getDisplayName());
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