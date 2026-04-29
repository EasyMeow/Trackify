package com.trackify.user.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackify.auth.application.AuthProperties;
import com.trackify.user.domain.User;
import com.trackify.user.infrastructure.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthProperties authProperties;

    private ChangePasswordService service;

    @BeforeEach
    void setUp() {
        AuthProperties.Password passwordConfig = new AuthProperties.Password(10, 8);
        lenient().when(authProperties.password()).thenReturn(passwordConfig);
        service = new ChangePasswordService(userRepository, passwordEncoder, authProperties);
    }

    @Test
    void successfulChange_updatesPasswordHash() {
        UUID id = UUID.randomUUID();
        User user = new User("alice", "alice@example.com", "old-hash", "Alice");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("newPassword1")).thenReturn("new-hash");
        when(userRepository.save(user)).thenReturn(user);

        service.changePassword(id, "old-password", "newPassword1");

        verify(userRepository).save(user);
        assert user.getPasswordHash().equals("new-hash");
    }

    @Test
    void wrongCurrentPassword_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        User user = new User("alice", "alice@example.com", "old-hash", "Alice");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(id, "wrong", "newPassword1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Current password is incorrect");
    }

    @Test
    void newPasswordTooShort_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        User user = new User("alice", "alice@example.com", "old-hash", "Alice");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(id, "old-password", "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8 characters");
    }
}
