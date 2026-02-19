package com.melina.notes.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
@Data
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String tag;

    @ManyToMany(mappedBy = "tags")
    private Set<Note> students = new HashSet<>();
}
