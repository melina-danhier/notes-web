package com.melina.notes.mapper;

import com.melina.notes.dto.TagDTO;
import com.melina.notes.entity.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {
    TagDTO toDTO(Tag tag);
}
