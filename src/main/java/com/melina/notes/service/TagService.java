package com.melina.notes.service;

import com.melina.notes.entity.Tag;
import com.melina.notes.exception.TagLimitExceededException;
import com.melina.notes.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TagService {
    static final int MAX_TAGS_PER_NOTE = 10;

    private final TagRepository tagRepository;

    public List<Tag> getOrCreateTags(List<String> rawTags) {
        Set<String> uniqueTagNames = normalizeTagNames(rawTags);
        if (uniqueTagNames.size() > MAX_TAGS_PER_NOTE) {
            throw new TagLimitExceededException(MAX_TAGS_PER_NOTE, uniqueTagNames.size());
        }

        List<Tag> tags = new ArrayList<>();

        for (String tagName : uniqueTagNames) {
            Tag tag = tagRepository.findByTag(tagName)
                    .orElseGet(() -> createTag(tagName));

            tags.add(tag);
        }

        return tags;
    }

    @Transactional(readOnly = true)
    public List<String> getAllTagsByUserId(long userId) {
        return tagRepository.findAllByUserId(userId)
                .stream()
                .map(Tag::getTag)
                .toList();
    }

    public void deleteAllWithNoNotes() {
        int del = tagRepository.deleteAllWithNoNotes();
        log.info("Deleted tags with no notes - deletedCount={}", del);
    }

    private Tag createTag(String tagName) {
        Tag tag = Tag.builder()
                .tag(tagName)
                .build();

        return tagRepository.save(tag);
    }

    private Set<String> normalizeTagNames(List<String> rawTags) {
        Set<String> uniqueTagNames = new LinkedHashSet<>();

        if (rawTags == null || rawTags.isEmpty()) {
            return uniqueTagNames;
        }

        for (String rawTag : rawTags) {
            if (rawTag == null) {
                continue;
            }

            String trimmedTag = rawTag.trim().toLowerCase(Locale.ROOT);

            if (!trimmedTag.isBlank()) {
                uniqueTagNames.add(trimmedTag);
            }
        }

        return uniqueTagNames;
    }
}
