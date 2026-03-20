package com.melina.notes.service;

import com.melina.notes.dto.TagDTO;
import com.melina.notes.entity.Tag;
import com.melina.notes.mapper.TagMapper;
import com.melina.notes.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public List<Tag> getOrCreateTags(List<TagDTO> tagStrings) {
        List<Tag> tags = new ArrayList<>();
        for (TagDTO tagDTO : tagStrings) {
            String tagString = tagDTO.getTag();
            Tag tag = tagRepository.findByTag(tagString).orElse(createTag(tagString));
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