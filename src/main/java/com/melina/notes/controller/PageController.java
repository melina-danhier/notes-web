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

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PageController {
    private final NoteService noteService;

    @GetMapping("/notes")
    public String notesPage(Model model, Authentication auth) {
        model.addAttribute("noteDto", new CreateUpdateNoteDTO());

        if (auth != null && auth.isAuthenticated()) {
            if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                List<NoteDTO> notes = noteService.getAllNotesByUserId(userDetails.getId());
                model.addAttribute("notes", notes);
                model.addAttribute("displayName", userDetails.getDisplayName());
                return "notes";
            }
        }
        throw new IllegalStateException("Benutzer nicht korrekt authentifiziert");
    }

}