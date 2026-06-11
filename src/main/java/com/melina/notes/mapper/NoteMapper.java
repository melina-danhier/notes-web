package com.melina.notes.mapper;

import com.melina.notes.dto.EditNoteDTO;
import com.melina.notes.dto.NoteDTO;
import com.melina.notes.entity.Note;
import com.melina.notes.util.ContentPreviewUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = TagMapper.class)
public interface NoteMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(target = "contentPreview", ignore = true)
    NoteDTO toNoteDTO(Note note);

    @AfterMapping
    default void setContentPreview(Note note, @MappingTarget NoteDTO noteDTO) {
        if (note.getContent() != null) {
            noteDTO.setContentPreview(ContentPreviewUtil.createPreview(note.getContent()));
        }
    }

    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "pinned", ignore = true)
    @Mapping(target = "archived", ignore = true)
    void updateNote(@MappingTarget Note note, EditNoteDTO noteDTO);

    @Mapping(target = "tagsRaw", ignore = true)
    EditNoteDTO toEditNoteDto(Note note);
}
