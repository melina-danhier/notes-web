package com.melina.notes.mapper;

import com.melina.notes.entity.Tag;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TagMapperTest {

    private final TagMapper tagMapper = Mappers.getMapper(TagMapper.class);

    @Test
    void toTagName_mapsName() {
        assertEquals("java", tagMapper.toTagName(Tag.builder().tag("java").build()));
    }

    @Test
    void toTagName_withNull_returnsNull() {
        assertNull(tagMapper.toTagName(null));
    }
}
