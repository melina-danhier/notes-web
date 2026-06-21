package com.melina.notes.controller;

import com.melina.notes.dto.NoteFormDto;
import com.melina.notes.dto.NotePageData;
import com.melina.notes.dto.NotePageRequest;
import com.melina.notes.dto.SignupDto;
import com.melina.notes.security.CustomUserDetails;
import com.melina.notes.service.NotePageService;
import com.melina.notes.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@SuppressWarnings("SameReturnValue")
@Controller
@RequiredArgsConstructor
public class PageController {

    private final NoteService noteService;
    private final NotePageService notePageService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        if (!model.containsAttribute("signupDto")) {
            model.addAttribute("signupDto", new SignupDto());
        }
        return "signup";
    }

    @GetMapping("/")
    public String homeToLogin() {
        return "login";
    }

    @GetMapping("/notes")
    public String notesPage(Model model,
                            @AuthenticationPrincipal CustomUserDetails user,
                            @ModelAttribute NotePageRequest request) {
        NotePageData pageData = notePageService.getActiveNotes(user.getId(), request);
        addPageDataToModel(model, pageData, user);
        return "notes";
    }

    @GetMapping("/notes/archive")
    public String archivePage(Model model,
                              @AuthenticationPrincipal CustomUserDetails user,
                              @ModelAttribute NotePageRequest request) {
        NotePageData pageData = notePageService.getArchivedNotes(user.getId(), request);
        addPageDataToModel(model, pageData, user);
        return "archive";
    }

    @GetMapping("/notes/trash")
    public String trashPage(Model model,
                            @AuthenticationPrincipal CustomUserDetails user,
                            @ModelAttribute NotePageRequest request) {
        NotePageData pageData = notePageService.getDeletedNotes(user.getId(), request);
        addPageDataToModel(model, pageData, user);
        return "trash";
    }

    @GetMapping("/notes/new")
    public String newNote(Model model) {
        model.addAttribute("noteDto", NoteFormDto.builder().build());
        model.addAttribute("tagsInputValue", "");
        return "note-view";
    }

    @GetMapping("/notes/{id}")
    public String editNote(@PathVariable Long id, Model model, @AuthenticationPrincipal CustomUserDetails user) {
        NoteFormDto note = noteService.getNoteByIdForView(user.getId(), id);
        model.addAttribute("noteDto", note);
        model.addAttribute("noteId", id);
        model.addAttribute("tagsInputValue", joinTags(note.tags()));
        return "note-view";
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(", ", tags);
    }

    private void addPageDataToModel(Model model, NotePageData pageData, CustomUserDetails user) {
        model.addAttribute("notes", pageData.notes());
        model.addAttribute("pinnedNotes", pageData.pinnedNotes());
        model.addAttribute("unpinnedNotes", pageData.unpinnedNotes());
        model.addAttribute("currentPage", pageData.currentPage());
        model.addAttribute("pageSize", pageData.pageSize());
        model.addAttribute("totalPages", pageData.notes().getTotalPages());
        model.addAttribute("sortField", pageData.sortField());
        model.addAttribute("sortDir", pageData.sortDir());
        model.addAttribute("displayname", user.getDisplayName());
        model.addAttribute("demoAccount", "ROLE_DEMO".equals(user.role()));
        model.addAttribute("allTags", pageData.allTags());
        model.addAttribute("tagFilter", pageData.tagFilter());
        model.addAttribute("search", pageData.search());
    }
}
