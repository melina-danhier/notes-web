package com.melina.notes.service;

import com.melina.notes.dto.TagDTO;
import com.melina.notes.entity.Tag;
import com.melina.notes.mapper.TagMapper;
import com.melina.notes.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private TagService tagService;

    @Test
    void getOrCreateTags_returnsEmptyListForBlankInput() {
        List<Tag> result = tagService.getOrCreateTags("   ");

        assertTrue(result.isEmpty());
    }

    @Test
    void getOrCreateTags_reusesExistingAndCreatesMissingTags() {
        Tag existingTag = Tag.builder().id(1L).tag("work").build();
        Tag createdTag = Tag.builder().id(2L).tag("new").build();

        when(tagRepository.findByTag("work")).thenReturn(Optional.of(existingTag));
        when(tagRepository.findByTag("new")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(createdTag);

        List<Tag> result = tagService.getOrCreateTags("work, new");

        assertEquals(2, result.size());
        assertSame(existingTag, result.get(0));
        assertSame(createdTag, result.get(1));

        verify(tagRepository, times(1)).save(any(Tag.class));
    }

    @Test
    void getAllTagsByUserId_removesOrphansAndMapsToDto() {
        Tag userTag = Tag.builder().id(3L).tag("backend").build();
        TagDTO dto = new TagDTO();
        dto.setId(3L);
        dto.setTag("backend");

        when(tagRepository.findAllByUserId(11L)).thenReturn(List.of(userTag));
        when(tagMapper.toDTO(userTag)).thenReturn(dto);

        List<TagDTO> result = tagService.getAllTagsByUserId(11L);

        verify(tagRepository).deleteAllWithNoNotes();
        assertEquals(1, result.size());
        assertSame(dto, result.get(0));
    }
}

