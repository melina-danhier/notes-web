package com.melina.notes.service;

import com.melina.notes.dto.TagDto;
import com.melina.notes.entity.Tag;
import com.melina.notes.mapper.TagMapper;
import com.melina.notes.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private TagService tagService;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n"})
    void getOrCreateTags_returnsEmptyListForBlankInputOrNull(String testTags) {
        List<Tag> result = tagService.getOrCreateTags(testTags);

        assertTrue(result.isEmpty());
        verifyNoInteractions(tagRepository);
    }

    @Test
    void getOrCreateTags_reusesExistingAndCreatesMissingTags() {
        Tag existingTag = Tag.builder()
                .id(1L)
                .tag("work")
                .build();

        Tag createdTag = Tag.builder()
                .id(2L)
                .tag("new")
                .build();

        when(tagRepository.findByTag("work")).thenReturn(Optional.of(existingTag));
        when(tagRepository.findByTag("new")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(createdTag);

        List<Tag> result = tagService.getOrCreateTags("work, new");

        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(tagCaptor.capture());

        Tag savedTag = tagCaptor.getValue();

        assertAll(
                () -> assertEquals("new", savedTag.getTag()),
                () -> assertEquals(2, result.size()),
                () -> assertSame(existingTag, result.getFirst()),
                () -> assertSame(createdTag, result.get(1))
        );

        verify(tagRepository).findByTag("work");
        verify(tagRepository).findByTag("new");
    }

    @Test
    void getOrCreateTags_trimsTagsBeforeSearching() {
        Tag existingTag = Tag.builder()
                .id(1L)
                .tag("work")
                .build();

        when(tagRepository.findByTag("work")).thenReturn(Optional.of(existingTag));

        List<Tag> result = tagService.getOrCreateTags("  work  ");

        assertEquals(1, result.size());
        assertSame(existingTag, result.getFirst());

        verify(tagRepository).findByTag("work");
        verify(tagRepository, never()).findByTag("  work  ");
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void getOrCreateTags_ignoresEmptyTagsBetweenCommas() {
        Tag existingTag = Tag.builder()
                .id(1L)
                .tag("work")
                .build();

        when(tagRepository.findByTag("work")).thenReturn(Optional.of(existingTag));

        List<Tag> result = tagService.getOrCreateTags("work, , ,");

        assertEquals(1, result.size());
        assertSame(existingTag, result.getFirst());

        verify(tagRepository, times(1)).findByTag("work");
        verify(tagRepository, times(1)).findByTag(anyString());
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void getOrCreateTags_removesDuplicateTagsAndKeepsOrder() {
        Tag workTag = Tag.builder()
                .id(1L)
                .tag("work")
                .build();

        Tag homeTag = Tag.builder()
                .id(2L)
                .tag("home")
                .build();

        when(tagRepository.findByTag("work")).thenReturn(Optional.of(workTag));
        when(tagRepository.findByTag("home")).thenReturn(Optional.of(homeTag));

        List<Tag> result = tagService.getOrCreateTags("work, home, work,  home ");

        assertEquals(2, result.size());
        assertSame(workTag, result.get(0));
        assertSame(homeTag, result.get(1));

        verify(tagRepository, times(1)).findByTag("work");
        verify(tagRepository, times(1)).findByTag("home");
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void getAllTagsByUserId_mapsTagsToDtos() {
        Tag userTag = Tag.builder()
                .id(3L)
                .tag("tag1")
                .build();

        TagDto dto = new TagDto();
        dto.setId(3L);
        dto.setTag("tag1");

        when(tagRepository.findAllByUserId(1L)).thenReturn(List.of(userTag));
        when(tagMapper.toDTO(userTag)).thenReturn(dto);

        List<TagDto> result = tagService.getAllTagsByUserId(1L);

        assertEquals(1, result.size());
        assertSame(dto, result.getFirst());

        verify(tagRepository).findAllByUserId(1L);
        verify(tagMapper).toDTO(userTag);
        verify(tagRepository, never()).deleteAllWithNoNotes();
    }

    @Test
    void deleteAllWithNoNotes_delegatesToRepository() {
        tagService.deleteAllWithNoNotes();

        verify(tagRepository).deleteAllWithNoNotes();
    }
}