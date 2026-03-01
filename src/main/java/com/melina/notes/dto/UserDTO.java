package com.melina.notes.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDTO {
    private String username;
    private String email;
    private String role;
    private List<NoteDTO> notes;
}
