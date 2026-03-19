package com.melina.notes.mapper;

import com.melina.notes.dto.NoteDTO;
import com.melina.notes.entity.Note;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NoteMapper {
    NoteDTO toNoteDTO(Note note);
    List<NoteDTO> toNoteDTO(List<Note> notes);
    void updateNote(@MappingTarget Note note, NoteDTO noteDTO);
}
