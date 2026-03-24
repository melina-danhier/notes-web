package com.melina.notes.service;

import com.melina.notes.entity.Tag;
import com.melina.notes.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;

    public List<Tag> getOrCreateTags(String rawTags) {
        if (rawTags == null || rawTags.trim().isEmpty()) {
            return new ArrayList<>(); // ✅ Handle null/empty
        }

        List<Tag> tags = new ArrayList<>();
        String[] rawTagsList = rawTags.split(",");

        for (String rawTag : rawTagsList) {
            String trimmedTag = rawTag.trim();
            if (trimmedTag.isEmpty()) continue; // ✅ Skip empty tags

            Tag tag = tagRepository.findByTag(trimmedTag)
                    .orElseGet(() -> createTag(trimmedTag)); // ✅ Use orElseGet
            tags.add(tag);
        }
        return tags;
    }

    private Tag createTag(String tagString) {
        Tag tag = Tag.builder() // ✅ Use builder if available
                .tag(tagString.trim())
                .build();
        return tagRepository.save(tag);
    }
}