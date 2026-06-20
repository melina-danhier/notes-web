package com.melina.notes.mapper;

import com.melina.notes.dto.NoteDto;
import com.melina.notes.dto.NoteFormDto;
import com.melina.notes.entity.Note;
import com.melina.notes.entity.Tag;
import com.melina.notes.entity.User;
import com.melina.notes.util.ContentPreviewUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {NoteMapperImpl.class, TagMapperImpl.class})
class NoteMapperTest {

    @Autowired
    private NoteMapper noteMapper;

    @Test
    void toNoteDto_mapsAllExposedFieldsAndCreatesPreview() {
        Instant created = Instant.parse("2026-06-18T10:15:30Z");
        Instant updated = Instant.parse("2026-06-19T11:16:31Z");
        String content = "Zeile 1\nZeile 2\nZeile 3\nZeile 4\nZeile 5\nZeile 6";
        Note note = Note.builder()
                .id(42L)
                .title("Mapper-Test")
                .content(content)
                .created(created)
                .updated(updated)
                .tags(List.of(Tag.builder().id(1L).tag("java").build()))
                .pinned(true)
                .archived(true)
                .build();

        NoteDto result = noteMapper.toNoteDto(note);

        assertAll(
                () -> assertEquals(42L, result.getId()),
                () -> assertEquals("Mapper-Test", result.getTitle()),
                () -> assertEquals(content, result.getContent()),
                () -> assertEquals(ContentPreviewUtil.createPreview(content), result.getContentPreview()),
                () -> assertEquals(created, result.getCreated()),
                () -> assertEquals(updated, result.getUpdated()),
                () -> assertEquals(List.of("java"), result.getTags()),
                () -> assertTrue(result.isPinned()),
                () -> assertTrue(result.isArchived())
        );
    }

    @Test
    void updateNote_updatesEditableFieldsAndPreservesManagedState() {
        Instant created = Instant.parse("2026-06-18T10:15:30Z");
        Instant updated = Instant.parse("2026-06-19T11:16:31Z");
        User user = User.builder().id(7L).build();
        List<Tag> tags = new ArrayList<>(List.of(Tag.builder().id(1L).tag("alt").build()));
        Note note = Note.builder()
                .id(42L)
                .title("Alt")
                .content("Alter Inhalt")
                .created(created)
                .updated(updated)
                .tags(tags)
                .user(user)
                .deleted(true)
                .pinned(true)
                .archived(true)
                .build();
        NoteFormDto form = new NoteFormDto("Neu", "Neuer Inhalt", List.of("neu"));

        noteMapper.updateNote(note, form);

        assertAll(
                () -> assertEquals("Neu", note.getTitle()),
                () -> assertEquals("Neuer Inhalt", note.getContent()),
                () -> assertEquals(42L, note.getId()),
                () -> assertEquals(created, note.getCreated()),
                () -> assertEquals(updated, note.getUpdated()),
                () -> assertSame(tags, note.getTags()),
                () -> assertSame(user, note.getUser()),
                () -> assertTrue(note.isDeleted()),
                () -> assertTrue(note.isPinned()),
                () -> assertTrue(note.isArchived())
        );
    }

    @Test
    void toNoteFormDto_mapsOnlyEditableFields() {
        Note note = Note.builder()
                .title("Titel")
                .content("Inhalt")
                .tags(List.of(Tag.builder().tag("work").build()))
                .build();

        NoteFormDto result = noteMapper.toNoteFormDto(note);

        assertAll(
                () -> assertEquals("Titel", result.title()),
                () -> assertEquals("Inhalt", result.content()),
                () -> assertEquals(List.of("work"), result.tags())
        );
    }

    @Test
    void mappingNullNote_returnsNull() {
        assertAll(
                () -> assertNull(noteMapper.toNoteDto(null)),
                () -> assertNull(noteMapper.toNoteFormDto(null))
        );
    }
}
