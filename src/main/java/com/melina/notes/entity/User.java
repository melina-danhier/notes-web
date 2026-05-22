package com.melina.notes.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {
    @Id
    @SequenceGenerator(name = "users_seq", sequenceName = "users_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    private Long id;

    private String username;
    private String email;
    private String password;
    private String role = "ROLE_USER";
}
