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
        List<Tag> tags = new ArrayList<>();
        String[] rawTagsList = rawTags.split(",");
        for (String rawTag : rawTagsList) {
            Tag tag = tagRepository
                    .findByTag(rawTag.trim())
                    .orElse(createTag(rawTag));
            tags.add(tag);
        }
        return tags;
    }

    private Tag createTag(String tagString) {
        Tag tag = new Tag();
        tag.setTag(tagString);
        return tagRepository.save(tag);
    }
}