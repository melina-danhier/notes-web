package com.melina.notes.mapper;

import com.melina.notes.entity.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {
    default String toTagName(Tag tag) {
        return tag == null ? null : tag.getTag();
    }
}
