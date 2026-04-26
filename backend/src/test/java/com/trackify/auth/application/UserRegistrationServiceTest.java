package com.trackify.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trackify.auth.domain.DuplicateLoginException;
import com.trackify.user.domain.User;
import com.trackify.user.infrastructure.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserRegistrationService service;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties(
                new AuthProperties.Password(10, 8),
                new AuthProperties.Bootstrap(new AuthProperties.Bootstrap.Admin("", "", "")));
        service = new UserRegistrationService(userRepository, passwordEncoder, properties);
    }

    @Test
    void registerHashesPasswordAndPersistsUser() {
        when(userRepository.existsByLogin("alice")).thenReturn(false);
        when(passwordEncoder.encode("hunter2!!")).thenReturn("BCRYPT-HASH");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = service.register(new RegisterUserCommand("alice", "alice@example.com", "Alice", "hunter2!!"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();
        assertThat(persisted.getLogin()).isEqualTo("alice");
        assertThat(persisted.getEmail()).isEqualTo("alice@example.com");
        assertThat(persisted.getDisplayName()).isEqualTo("Alice");
        assertThat(persisted.getPasswordHash()).isEqualTo("BCRYPT-HASH");
        assertThat(persisted.getPasswordHash()).isNotEqualTo("hunter2!!");
        assertThat(saved).isSameAs(persisted);
        verify(passwordEncoder, times(1)).encode("hunter2!!");
    }

    @Test
    void registerRejectsDuplicateLoginBeforeHashingOrSaving() {
        when(userRepository.existsByLogin("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.register(
                new RegisterUserCommand("alice", "alice@example.com", "Alice", "hunter2!!")))
                .isInstanceOf(DuplicateLoginException.class)
                .hasMessageContaining("alice");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRejectsPasswordShorterThanConfiguredMinLength() {
        assertThatThrownBy(() -> service.register(
                new RegisterUserCommand("alice", "alice@example.com", "Alice", "short")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8");

        verify(userRepository, never()).existsByLogin(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
