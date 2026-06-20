package com.melina.notes.mapper;

import com.melina.notes.dto.NoteFormDto;
import com.melina.notes.dto.NoteDto;
import com.melina.notes.entity.Note;
import com.melina.notes.util.ContentPreviewUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        uses = TagMapper.class,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface NoteMapper {
    @Mapping(target = "contentPreview", ignore = true)
    NoteDto toNoteDto(Note note);

    @AfterMapping
    default void setContentPreview(Note note, @MappingTarget NoteDto noteDTO) {
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
    @Mapping(target = "id", ignore = true)
    void updateNote(@MappingTarget Note note, NoteFormDto noteForm);

    NoteFormDto toNoteFormDto(Note note);
}
