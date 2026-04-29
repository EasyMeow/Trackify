package com.trackify.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.user.domain.User;
import com.trackify.user.infrastructure.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@WebMvcTest(controllers = AvatarController.class)
@Import({SecurityConfig.class, WebConfig.class, JacksonConfig.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true"
})
class AvatarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    private Authentication auth() {
        UUID userId = UUID.randomUUID();
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    @Test
    void getAvatar_userHasAvatar_returns200WithBytes() throws Exception {
        UUID id = UUID.randomUUID();
        User user = new User("alice", "alice@example.com", "hash", "Alice");
        user.setAvatar(new byte[]{1, 2, 3});
        user.setAvatarContentType("image/jpeg");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/{id}/avatar", id).with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void getAvatar_noAvatarSet_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        User user = new User("bob", "bob@example.com", "hash", "Bob");
        // avatar is null by default
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/{id}/avatar", id).with(authentication(auth())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAvatar_userNotFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{id}/avatar", id).with(authentication(auth())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAvatar_unauthenticated_returns401() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/users/{id}/avatar", id))
                .andExpect(status().isUnauthorized());
    }
}
