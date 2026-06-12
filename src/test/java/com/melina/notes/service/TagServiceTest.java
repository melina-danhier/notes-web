package com.melina.notes.service;

import com.melina.notes.dto.TagDto;
import com.melina.notes.entity.Tag;
import com.melina.notes.mapper.TagMapper;
import com.melina.notes.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
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
    @NullSource
    @EmptySource
    void getOrCreateTags_blankOrNullInput_returnsEmptyList(List<String> rawTags) {
        List<Tag> result = tagService.getOrCreateTags(rawTags);

        assertTrue(result.isEmpty());
        verifyNoInteractions(tagRepository);
    }

    @Test
    void getOrCreateTags_reusesExistingTagAndCreatesMissingTag() {
        Tag existingTag = createTag(1L, "work");
        Tag createdTag = createTag(2L, "new");

        when(tagRepository.findByTag("work")).thenReturn(Optional.of(existingTag));
        when(tagRepository.findByTag("new")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(createdTag);

        List<Tag> result = tagService.getOrCreateTags(List.of("work", "new"));

        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(tagCaptor.capture());

        Tag tagToSave = tagCaptor.getValue();

        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertSame(existingTag, result.getFirst()),
                () -> assertSame(createdTag, result.get(1)),
                () -> assertEquals("new", tagToSave.getTag())
        );

        verify(tagRepository).findByTag("work");
        verify(tagRepository).findByTag("new");
    }

    @Test
    void getOrCreateTags_trimsTagsBeforeSearching() {
        Tag existingTag = createTag(1L, "work");

        when(tagRepository.findByTag("work")).thenReturn(Optional.of(existingTag));

        List<Tag> result = tagService.getOrCreateTags(List.of("  work  "));

        assertEquals(1, result.size());
        assertSame(existingTag, result.getFirst());

        verify(tagRepository).findByTag("work");
        verify(tagRepository, never()).findByTag("  work  ");
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void getOrCreateTags_ignoresEmptyTagsBetweenCommas() {
        Tag existingTag = createTag(1L, "work");

        when(tagRepository.findByTag("work")).thenReturn(Optional.of(existingTag));

        List<Tag> result = tagService.getOrCreateTags(List.of("work", "", "   ", ""));

        assertEquals(1, result.size());
        assertSame(existingTag, result.getFirst());

        verify(tagRepository, only()).findByTag("work");
    }

    @Test
    void getOrCreateTags_removesDuplicateTagsAndKeepsOrder() {
        Tag workTag = createTag(1L, "work");
        Tag homeTag = createTag(2L, "home");

        when(tagRepository.findByTag("work")).thenReturn(Optional.of(workTag));
        when(tagRepository.findByTag("home")).thenReturn(Optional.of(homeTag));

        List<Tag> result = tagService.getOrCreateTags(List.of("work", "home", "work", "  home "));

        assertEquals(2, result.size());
        assertSame(workTag, result.get(0));
        assertSame(homeTag, result.get(1));

        verify(tagRepository, times(1)).findByTag("work");
        verify(tagRepository, times(1)).findByTag("home");
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void getOrCreateTags_normalizesCaseBeforeRemovingDuplicates() {
        Tag workTag = createTag(1L, "work");

        when(tagRepository.findByTag("work")).thenReturn(Optional.of(workTag));

        List<Tag> result = tagService.getOrCreateTags(List.of("Work", "WORK", " work "));

        assertEquals(List.of(workTag), result);
        verify(tagRepository, only()).findByTag("work");
    }

    @Test
    void getAllTagsByUserId_mapsTagsToDtos() {
        Tag tag = createTag(3L, "tag1");

        TagDto dto = new TagDto();
        dto.setId(3L);
        dto.setTag("tag1");

        when(tagRepository.findAllByUserId(1L)).thenReturn(List.of(tag));
        when(tagMapper.toDTO(tag)).thenReturn(dto);

        List<TagDto> result = tagService.getAllTagsByUserId(1L);

        assertEquals(1, result.size());
        assertSame(dto, result.getFirst());

        verify(tagRepository).findAllByUserId(1L);
        verify(tagMapper).toDTO(tag);
        verify(tagRepository, never()).deleteAllWithNoNotes();
    }

    @Test
    void deleteAllWithNoNotes_delegatesToRepository() {
        tagService.deleteAllWithNoNotes();

        verify(tagRepository).deleteAllWithNoNotes();
        verifyNoMoreInteractions(tagRepository);
    }

    private Tag createTag(Long id, String tagName) {
        return Tag.builder()
                .id(id)
                .tag(tagName)
                .build();
    }
}
