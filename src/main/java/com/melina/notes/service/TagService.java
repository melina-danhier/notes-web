package com.melina.notes.service;

import com.melina.notes.dto.TagDto;
import com.melina.notes.entity.Tag;
import com.melina.notes.mapper.TagMapper;
import com.melina.notes.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public List<Tag> getOrCreateTags(String rawTags) {
        Set<String> uniqueTagNames = parseTagNames(rawTags);

        List<Tag> tags = new ArrayList<>();

        for (String tagName : uniqueTagNames) {
            Tag tag = tagRepository.findByTag(tagName)
                    .orElseGet(() -> createTag(tagName));

            tags.add(tag);
        }

        return tags;
    }

    @Transactional(readOnly = true)
    public List<TagDto> getAllTagsByUserId(long userId) {
        return tagRepository.findAllByUserId(userId)
                .stream()
                .map(tagMapper::toDTO)
                .toList();
    }

    public void deleteAllWithNoNotes() {
        tagRepository.deleteAllWithNoNotes();
    }

    private Tag createTag(String tagName) {
        Tag tag = Tag.builder()
                .tag(tagName)
                .build();

        return tagRepository.save(tag);
    }

    private Set<String> parseTagNames(String rawTags) {
        Set<String> uniqueTagNames = new LinkedHashSet<>();

        if (rawTags == null || rawTags.isBlank()) {
            return uniqueTagNames;
        }

        String[] rawTagsList = rawTags.split(",");

        for (String rawTag : rawTagsList) {
            String trimmedTag = rawTag.trim();

            if (!trimmedTag.isBlank()) {
                uniqueTagNames.add(trimmedTag);
            }
        }

        return uniqueTagNames;
    }
}