package com.melina.notes.config;

import com.melina.notes.entity.Note;
import com.melina.notes.entity.Tag;
import com.melina.notes.entity.User;
import com.melina.notes.repository.NoteRepository;
import com.melina.notes.repository.TagRepository;
import com.melina.notes.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private NoteRepository noteRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsDemoUserTagsAndNotes() {
        when(userRepository.existsByEmail("demo@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secure-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tagRepository.findByTag(anyString())).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        seeder("secure-password").run(new DefaultApplicationArguments());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Note>> notesCaptor = ArgumentCaptor.forClass(List.class);
        verify(noteRepository).saveAll(notesCaptor.capture());

        List<Note> notes = notesCaptor.getValue();
        assertThat(notes).hasSize(5);
        assertThat(notes).anyMatch(Note::isPinned);
        assertThat(notes).anyMatch(Note::isArchived);
        assertThat(notes).anyMatch(Note::isDeleted);
    }

    @Test
    void skipsSeedingWhenDemoUserAlreadyExists() {
        when(userRepository.existsByEmail("demo@example.com")).thenReturn(true);

        seeder("secure-password").run(new DefaultApplicationArguments());

        verify(userRepository, never()).save(any());
        verify(noteRepository, never()).saveAll(any());
    }

    @Test
    void rejectsMissingPassword() {
        assertThatThrownBy(() -> seeder(" ").run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO_USER_PASSWORD");
    }

    private DemoDataSeeder seeder(String password) {
        return new DemoDataSeeder(
                userRepository,
                noteRepository,
                tagRepository,
                passwordEncoder,
                "Demo User",
                "DEMO@example.com",
                password
        );
    }
}
