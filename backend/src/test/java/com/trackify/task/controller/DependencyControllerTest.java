package com.trackify.task.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackify.auth.application.LocalUserDetailsService;
import com.trackify.auth.application.LocalUserPrincipal;
import com.trackify.common.exception.ConflictException;
import com.trackify.common.exception.ForbiddenException;
import com.trackify.common.exception.NotFoundException;
import com.trackify.config.JacksonConfig;
import com.trackify.config.SecurityConfig;
import com.trackify.config.WebConfig;
import com.trackify.task.application.TaskDependencyService;
import com.trackify.task.dto.CreateDependencyRequest;
import com.trackify.task.dto.DependencyResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * TASK-089: verifies {@code POST /api/tasks/{taskId}/dependencies} and
 * {@code DELETE /api/tasks/{taskId}/dependencies/{dependencyId}} behaviour.
 */
@WebMvcTest(controllers = DependencyController.class)
@Import({SecurityConfig.class, WebConfig.class, JacksonConfig.class})
@TestPropertySource(properties = {
        "trackify.cors.allowed-origin=http://localhost:5173",
        "trackify.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS",
        "trackify.cors.allowed-headers=*",
        "trackify.cors.allow-credentials=true"
})
class DependencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskDependencyService taskDependencyService;

    @MockitoBean
    private LocalUserDetailsService localUserDetailsService;

    private static RequestPostProcessor auth(UUID userId) {
        LocalUserPrincipal principal = new LocalUserPrincipal(userId, "alice", "hashed-pw");
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @Test
    void authenticatedMemberCanCreateDependencyAndReceives201() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID predecessorId = UUID.randomUUID();
        UUID dependencyId = UUID.randomUUID();

        DependencyResponse stub = new DependencyResponse(dependencyId, predecessorId, taskId, Instant.now());
        when(taskDependencyService.create(eq(taskId), eq(userId), any(CreateDependencyRequest.class)))
                .thenReturn(stub);

        String body = objectMapper.writeValueAsString(new CreateDependencyRequest(predecessorId));

        mockMvc.perform(post("/api/tasks/{taskId}/dependencies", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(dependencyId.toString()))
                .andExpect(jsonPath("$.predecessorTaskId").value(predecessorId.toString()))
                .andExpect(jsonPath("$.successorTaskId").value(taskId.toString()));

        verify(taskDependencyService).create(eq(taskId), eq(userId), any(CreateDependencyRequest.class));
    }

    @Test
    void createDependencyMissingPredecessorIdReturns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        mockMvc.perform(post("/api/tasks/{taskId}/dependencies", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        verifyNoInteractions(taskDependencyService);
    }

    @Test
    void createDependencyForbiddenWhenCallerLacksAccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID predecessorId = UUID.randomUUID();

        when(taskDependencyService.create(eq(taskId), eq(userId), any(CreateDependencyRequest.class)))
                .thenThrow(new ForbiddenException("Project not accessible"));

        String body = objectMapper.writeValueAsString(new CreateDependencyRequest(predecessorId));

        mockMvc.perform(post("/api/tasks/{taskId}/dependencies", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void createDependencyNotFoundWhenTaskMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID predecessorId = UUID.randomUUID();

        when(taskDependencyService.create(eq(taskId), eq(userId), any(CreateDependencyRequest.class)))
                .thenThrow(new NotFoundException("Task not found"));

        String body = objectMapper.writeValueAsString(new CreateDependencyRequest(predecessorId));

        mockMvc.perform(post("/api/tasks/{taskId}/dependencies", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void createDependencyConflictWhenDuplicate() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID predecessorId = UUID.randomUUID();

        when(taskDependencyService.create(eq(taskId), eq(userId), any(CreateDependencyRequest.class)))
                .thenThrow(new ConflictException("Dependency already exists"));

        String body = objectMapper.writeValueAsString(new CreateDependencyRequest(predecessorId));

        mockMvc.perform(post("/api/tasks/{taskId}/dependencies", taskId)
                        .with(auth(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void createDependencyUnauthenticatedReturns401() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID predecessorId = UUID.randomUUID();

        String body = objectMapper.writeValueAsString(new CreateDependencyRequest(predecessorId));

        mockMvc.perform(post("/api/tasks/{taskId}/dependencies", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskDependencyService);
    }

    @Test
    void authenticatedMemberCanDeleteDependencyAndReceives204() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID dependencyId = UUID.randomUUID();

        doNothing().when(taskDependencyService).delete(eq(taskId), eq(dependencyId), eq(userId));

        mockMvc.perform(delete("/api/tasks/{taskId}/dependencies/{dependencyId}", taskId, dependencyId)
                        .with(auth(userId)))
                .andExpect(status().isNoContent());

        verify(taskDependencyService).delete(eq(taskId), eq(dependencyId), eq(userId));
    }

    @Test
    void deleteDependencyNotFoundWhenMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID dependencyId = UUID.randomUUID();

        doThrow(new NotFoundException("Dependency not found"))
                .when(taskDependencyService).delete(eq(taskId), eq(dependencyId), eq(userId));

        mockMvc.perform(delete("/api/tasks/{taskId}/dependencies/{dependencyId}", taskId, dependencyId)
                        .with(auth(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void deleteDependencyUnauthenticatedReturns401() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID dependencyId = UUID.randomUUID();

        mockMvc.perform(delete("/api/tasks/{taskId}/dependencies/{dependencyId}", taskId, dependencyId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskDependencyService);
    }
}
