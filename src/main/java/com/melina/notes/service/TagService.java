package com.melina.notes.service;

import com.melina.notes.dto.TagDto;
import com.melina.notes.entity.Tag;
import com.melina.notes.mapper.TagMapper;
import com.melina.notes.repository.TagRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public List<Tag> getOrCreateTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return new ArrayList<>();
        }

        Set<String> uniqueTagNames = new LinkedHashSet<>();
        String[] rawTagsList = rawTags.split(",");
        for (String rawTag : rawTagsList) {
            String trimmedTag = rawTag.trim();

            if (!trimmedTag.isBlank()) {
                uniqueTagNames.add(trimmedTag);
            }
        }

        List<Tag> tags = new ArrayList<>();
        for (String tagName : uniqueTagNames) {
            Tag tag = tagRepository.findByTag(tagName)
                    .orElseGet(() -> createTag(tagName));

            tags.add(tag);
        }

        return tags;
    }

    private Tag createTag(String tagString) {
        Tag tag = Tag.builder()
                .tag(tagString.trim())
                .build();
        return tagRepository.save(tag);
    }

    @Transactional
    public List<TagDto> getAllTagsByUserId(long userId) {
        tagRepository.deleteAllWithNoNotes();
        return tagRepository.findAllByUserId(userId)
                .stream()
                .map(tagMapper::toDTO)
                .toList();
    }

}