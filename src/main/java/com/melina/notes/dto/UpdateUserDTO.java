package com.melina.notes.dto;

import lombok.Data;

@Data
public class UpdateUserDTO {
    private String username;
    private String email;
    private String password;
}
