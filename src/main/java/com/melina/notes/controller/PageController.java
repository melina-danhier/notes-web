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
    public String login(Model model,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String registered) {
        if (error != null) {
            model.addAttribute("error", "Ungültige E-Mail oder Passwort");
        }
        if (logout != null) {
            model.addAttribute("logout", "Sie wurden erfolgreich abgemeldet");
        }
        if (registered != null) {
            model.addAttribute("success", "Registrierung erfolgreich! Bitte melden Sie sich an.");
        }
        return "login";
    }

    @GetMapping("/signup")
    public String signup(Model model, @RequestParam(required = false) String error) {
        String errorMessage = null;
        if (error != null) {
            errorMessage = switch(error) {
                case "empty_name" -> "Bitte geben Sie Ihren Namen ein";
                case "empty_email" -> "Bitte geben Sie Ihre E-Mail-Adresse ein";
                case "empty_password" -> "Bitte geben Sie ein Passwort ein";
                case "password_too_short" -> "Das Passwort muss mindestens 6 Zeichen lang sein";
                case "password_mismatch" -> "Passwörter stimmen nicht überein";
                case "email_exists" -> "Diese E-Mail-Adresse ist bereits registriert";
                case "registration_failed" -> "Registrierung fehlgeschlagen. Bitte versuchen Sie es erneut";
                default -> "Ein Fehler ist aufgetreten";
            };
            model.addAttribute("error", errorMessage);
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
                            @RequestParam(required = false, defaultValue = "0") int pageNo,
                            @RequestParam(required = false, defaultValue = "6") int pageSize,
                            @RequestParam(required = false, defaultValue = "created") String sortField,
                            @RequestParam(required = false, defaultValue = "desc") String sortDir,
                            @RequestParam(required = false) String tagFilter,
                            @RequestParam(required = false) String search) {
        NoteFilter filter = NoteFilter.builder()
                .userId(user.getId())
                .tagFilter(tagFilter)
                .searchTerm(search)
                .deleted(false)
                .build();
        addToModel(model, sortDir, sortField, filter, user, pageNo, pageSize, tagFilter, search, false);
        return "notes";
    }

    @GetMapping("/notes/trash")
    public String trashPage(Model model,
                            @AuthenticationPrincipal CustomUserDetails user,
                            @RequestParam(required = false, defaultValue = "0") int pageNo,
                            @RequestParam(required = false, defaultValue = "6") int pageSize,
                            @RequestParam(required = false, defaultValue = "created") String sortField,
                            @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        NoteFilter filter = NoteFilter.builder()
                .userId(user.getId())
                .deleted(true)
                .build();
        addToModel(model, sortDir, sortField, filter, user, pageNo, pageSize, null, null, true);
        return "trash";
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

    private void addToModel(Model model, String sortDir, String sortField, NoteFilter filter, CustomUserDetails user,
                            int pageNo, int pageSize, String tagFilter, String search, boolean trashPage) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortField);
        Page<NoteDTO> notes = noteService.getAllNotes(filter, PageRequest.of(pageNo, pageSize, sort));

        model.addAttribute("notes", notes);
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", notes.getTotalPages());
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("displayname", user.getDisplayName());

        if (!trashPage) {
            List<TagDTO> tags = tagService.getAllTagsByUserId(user.getId());
            model.addAttribute("allTags", tags);
            model.addAttribute("tagFilter", tagFilter);
            model.addAttribute("search", search);
        }
    }
}