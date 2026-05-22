package com.melina.notes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag {
    @Id
    @SequenceGenerator(name = "tags_seq", sequenceName = "tags_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tags_seq")
    private Long id;

    private String tag;

    @ManyToMany(mappedBy = "tags")
    @Builder.Default
    private Set<Note> notes = new HashSet<>();

}
