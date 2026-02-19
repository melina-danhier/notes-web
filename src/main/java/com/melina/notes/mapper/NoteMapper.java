package com.melina.notes.mapper;

import com.melina.notes.dto.CreateUpdateNoteDto;
import com.melina.notes.dto.NoteDto;
import com.melina.notes.entity.Note;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NoteMapper {
    Note toEntity(CreateUpdateNoteDto dto);
    NoteDto toDto(Note note);
    void update(@MappingTarget Note note, CreateUpdateNoteDto dto);
}
