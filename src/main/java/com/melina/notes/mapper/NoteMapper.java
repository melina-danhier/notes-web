package com.melina.notes.mapper;

import com.melina.notes.dto.CreateUpdateNoteDTO;
import com.melina.notes.dto.NoteDTO;
import com.melina.notes.entity.Note;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = TagMapper.class)
public interface NoteMapper {
    @Mapping(source = "user.id", target = "userId")
    NoteDTO toNoteDTO(Note note);

    List<NoteDTO> toNoteDTO(List<Note> notes);

    @Mapping(target = "tags", ignore = true)
    void updateNote(@MappingTarget Note note, CreateUpdateNoteDTO noteDTO);
}
