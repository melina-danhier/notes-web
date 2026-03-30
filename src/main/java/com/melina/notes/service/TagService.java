package com.melina.notes.service;

import com.melina.notes.dto.TagDTO;
import com.melina.notes.entity.Tag;
import com.melina.notes.mapper.TagMapper;
import com.melina.notes.repository.TagRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public List<Tag> getOrCreateTags(String rawTags) {
        if (rawTags == null || rawTags.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Tag> tags = new ArrayList<>();
        String[] rawTagsList = rawTags.split(",");

        for (String rawTag : rawTagsList) {
            String trimmedTag = rawTag.trim();
            if (trimmedTag.isEmpty()) continue;

            Tag tag = tagRepository.findByTag(trimmedTag)
                    .orElseGet(() -> createTag(trimmedTag));
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
    public List<TagDTO> getAllTagsByUserId(long userId) {
        tagRepository.deleteAllWithNoNotes();
        return tagRepository.findAllByUserId(userId)
                .stream()
                .map(tagMapper::toDTO)
                .toList();
    }
}