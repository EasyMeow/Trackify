package com.trackify.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackify.user.domain.User;
import com.trackify.user.dto.MeResponse;
import com.trackify.user.infrastructure.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class UpdateMeServiceTest {

    @Mock
    private UserRepository userRepository;

    private UpdateMeService service;

    @BeforeEach
    void setUp() {
        service = new UpdateMeService(userRepository);
    }

    @Test
    void displayNameUpdate_setsDisplayName() {
        UUID id = UUID.randomUUID();
        User user = new User("alice", "alice@example.com", "hash", "Alice");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        MeResponse result = service.update(id, "Alice Updated", null);

        assertThat(result.displayName()).isEqualTo("Alice Updated");
        verify(userRepository).save(user);
    }

    @Test
    void avatarUpload_storesBytesAndContentType() throws Exception {
        UUID id = UUID.randomUUID();
        User user = new User("alice", "alice@example.com", "hash", "Alice");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        byte[] bytes = new byte[]{10, 20, 30};
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.png", "image/png", bytes);

        service.update(id, null, file);

        assertThat(user.getAvatar()).isEqualTo(bytes);
        assertThat(user.getAvatarContentType()).isEqualTo("image/png");
    }

    @Test
    void oversizedAvatar_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        User user = new User("alice", "alice@example.com", "hash", "Alice");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        byte[] bigBytes = new byte[1_048_577];
        MockMultipartFile file = new MockMultipartFile("avatar", "big.jpg", "image/jpeg", bigBytes);

        assertThatThrownBy(() -> service.update(id, null, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Avatar file must not exceed 1 MB");
    }

    @Test
    void wrongContentType_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        User user = new User("alice", "alice@example.com", "hash", "Alice");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        MockMultipartFile file = new MockMultipartFile("avatar", "file.gif", "image/gif", new byte[100]);

        assertThatThrownBy(() -> service.update(id, null, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Avatar must be JPEG, PNG, or WebP");
    }
}
