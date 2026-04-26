package com.trackify.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackify.common.HealthController;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proves the TASK-024 filter chain: {@code /api/health} is public and any other
 * protected {@code /api/**} path returns 401 when there is no authenticated
 * session. {@code /api/me} is used as the protected probe — it is not yet
 * implemented (TASK-028 owns it) but Spring Security's authorization filter
 * runs before the dispatcher servlet and returns 401 regardless of whether the
 * path is mapped to a controller.
 */
@WebMvcTest(controllers = HealthController.class)
@Import({SecurityConfig.class, WebConfig.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true"
})
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedRouteReturnsUnauthorizedWhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }
}
