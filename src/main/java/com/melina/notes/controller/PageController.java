package com.melina.notes.controller;

import com.melina.notes.dto.CreateUpdateNoteDTO;
import com.melina.notes.dto.NoteDTO;
import com.melina.notes.service.NoteService;
import com.melina.notes.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final NoteService noteService;

    @GetMapping("/notes")
    public String notesPage(Model model, Authentication auth) {
        model.addAttribute("noteDto", new CreateUpdateNoteDTO());

        if (auth != null && auth.isAuthenticated()) {
            Long userId = getUserIdFromAuth(auth);
            List<NoteDTO> notes = noteService.getAllNotes(userId);
            model.addAttribute("notes", notes);

            // 👈 ADD THIS - Username für Template
            if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                model.addAttribute("displayName", userDetails.getDisplayName());
            }
        }
        return "notes";
    }

    private Long getUserIdFromAuth(Authentication auth) {
        if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }
        throw new IllegalStateException("Benutzer nicht authentifiziert");
    }
}