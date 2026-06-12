package com.melina.notes.mapper;

import com.melina.notes.dto.TagDto;
import com.melina.notes.entity.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagDto toDTO(Tag tag);
}
