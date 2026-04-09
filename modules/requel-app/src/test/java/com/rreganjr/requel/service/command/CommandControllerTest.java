/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.requel.service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.repository.jpa.BeanValidationException;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.service.stream.StreamEventPublisher;
import com.rreganjr.validator.EntityValidationException;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer tests for {@link CommandController}.
 *
 * Uses {@code @WebMvcTest} so only the controller, Jackson, and Spring Security
 * are loaded — no JPA, no Spring context. All collaborators are mocked.
 *
 * Security: {@code ApiSecurityConfig} requires {@code JwtService} to wire the
 * JWT filter. {@code @MockBean JwtService} satisfies that dependency.
 * {@code @WithMockUser} pre-populates the {@code SecurityContext} so requests
 * are treated as authenticated without a real JWT token.
 *
 * Scenarios covered:
 * - Happy path: JSON command with result DTO → 200 with success envelope
 * - Happy path: command with no result extractor → 200 with null entity
 * - Unknown command type → 400 Bad Request
 * - AuthorizationException → 403 Forbidden
 * - OptimisticLockException → 409 Conflict
 * - EntityValidationException → 422 Unprocessable Entity with violations list
 * - BeanValidationException → 422 with per-field violations
 * - EntityException (non-validation) → 409 Conflict
 * - Unhandled exception → 500 Internal Server Error (no internal detail exposed)
 * - ProjectScopedCommand success → SSE broadcast event published for Project:0
 * - Result DTO with id() method → targeted SSE event published for that entity
 * - Non-project command → no SSE broadcast published
 */
/**
 * Note on test approach: {@code @WebMvcTest} does not work cleanly here because
 * the application's {@code application-config.xml} (loaded via {@code @ImportResource})
 * does a full component scan of {@code com.rreganjr}, which pulls in JPA-dependent beans
 * that cannot be satisfied in a web-slice context. Using {@code @SpringBootTest} with
 * a mock web environment and {@code @AutoConfigureMockMvc} gives the same MockMvc-based
 * HTTP testing with the full working context (H2 in test profile) while still mocking
 * the specific collaborators under test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class CommandControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ApiCommandFactory apiCommandFactory;
    @MockBean CommandHandler commandHandler;
    @MockBean StreamEventPublisher streamEventPublisher;

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    void jsonCommandWithResultReturnsOk() throws Exception {
        record GoalDto(Long id, String name) {}
        GoalDto dto = new GoalDto(42L, "My Goal");

        Command cmd = stubCommand("EditGoal", dto);

        mockMvc.perform(post("/api/commands/EditGoal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.entityType").value("EditGoal"))
                .andExpect(jsonPath("$.entity.id").value(42))
                .andExpect(jsonPath("$.entity.name").value("My Goal"));

        verify(commandHandler).execute(cmd);
    }

    @Test
    void commandWithNoResultExtractorReturnsOkWithNullEntity() throws Exception {
        Command cmd = stubCommand("DeleteGoal", null);

        mockMvc.perform(post("/api/commands/DeleteGoal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.entityType").value("DeleteGoal"))
                .andExpect(jsonPath("$.entity").doesNotExist());
    }

    // -------------------------------------------------------------------------
    // Error mapping
    // -------------------------------------------------------------------------

    @Test
    void unknownCommandTypeReturnsBadRequest() throws Exception {
        when(apiCommandFactory.getInputType("UnknownCommand"))
                .thenThrow(new IllegalArgumentException("No command registered: UnknownCommand"));

        mockMvc.perform(post("/api/commands/UnknownCommand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void authorizationExceptionReturnsForbidden() throws Exception {
        stubCommandThrows("EditGoal", new AuthorizationException("Not allowed"));

        mockMvc.perform(post("/api/commands/EditGoal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void optimisticLockExceptionReturnsConflict() throws Exception {
        stubCommandThrows("EditGoal", new OptimisticLockException("stale"));

        mockMvc.perform(post("/api/commands/EditGoal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void entityValidationExceptionReturnsUnprocessableEntity() throws Exception {
        EntityValidationException ex = EntityValidationException.validationFailed(
                String.class, "name", "Name cannot be blank");
        stubCommandThrows("EditGoal", ex);

        mockMvc.perform(post("/api/commands/EditGoal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(1));
    }

    @Test
    void beanValidationExceptionReturnsUnprocessableEntityWithFieldViolations() throws Exception {
        BeanValidationException ex = new BeanValidationException(
                new RuntimeException("cause"),
                String.class, null,
                new String[]{"username", "password"},
                new String[]{"must not be blank", "must be at least 8 characters"},
                EntityExceptionActionType.Unknown,
                "validation failed");
        stubCommandThrows("EditUser", ex);

        mockMvc.perform(post("/api/commands/EditUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.violations.length()").value(2))
                .andExpect(jsonPath("$.violations[0].field").value("username"))
                .andExpect(jsonPath("$.violations[0].message").value("must not be blank"))
                .andExpect(jsonPath("$.violations[1].field").value("password"))
                .andExpect(jsonPath("$.violations[1].message").value("must be at least 8 characters"));
    }

    @Test
    void entityExceptionReturnsConflict() throws Exception {
        EntityException ex = EntityException.forUnknownProblem(
                new RuntimeException("db error"), String.class, null,
                EntityExceptionActionType.Unknown);
        stubCommandThrows("EditGoal", ex);

        mockMvc.perform(post("/api/commands/EditGoal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void unhandledExceptionReturnsInternalServerError() throws Exception {
        stubCommandThrows("EditGoal", new RuntimeException("database on fire"));

        mockMvc.perform(post("/api/commands/EditGoal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                // Raw exception message must not leak to the client
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again or contact support."));
    }

    // -------------------------------------------------------------------------
    // SSE event publishing
    // -------------------------------------------------------------------------

    @Test
    void projectScopedCommandPublishesBroadcastSseEvent() throws Exception {
        Project project = mock(Project.class);
        when(project.getId()).thenReturn(55L);

        // Command implements both Command and ProjectScopedCommand
        Command cmd = mock(Command.class, withSettings().extraInterfaces(ProjectScopedCommand.class));
        when(((ProjectScopedCommand) cmd).getProject()).thenReturn(project);

        doReturn(Void.class).when(apiCommandFactory).getInputType("EditGoal");
        when(apiCommandFactory.newCommand(eq("EditGoal"), any(), any())).thenReturn(cmd);
        when(commandHandler.execute(cmd)).thenReturn(cmd);
        when(apiCommandFactory.extractResult(eq("EditGoal"), any())).thenReturn(null);

        mockMvc.perform(post("/api/commands/EditGoal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // Broadcast event published to Project:0 (sidebar refresh signal)
        verify(streamEventPublisher).publishTargetUpdate(eq("Project"), eq(0L), any());
    }

    @Test
    void resultDtoWithIdMethodPublishesTargetedSseEvent() throws Exception {
        record GoalDto(Long id, String name) {}
        GoalDto dto = new GoalDto(99L, "SSE Goal");
        stubCommand("EditGoal", dto);

        mockMvc.perform(post("/api/commands/EditGoal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // Targeted event published: controller strips "Dto" suffix (GoalDto → "Goal")
        verify(streamEventPublisher).publishTargetUpdate(eq("Goal"), eq(99L), any());
    }

    @Test
    void nonProjectCommandDoesNotPublishBroadcastSseEvent() throws Exception {
        stubCommand("EditUser", null);

        mockMvc.perform(post("/api/commands/EditUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // No broadcast: EditUser is not ProjectScopedCommand or EditProjectOrDomainEntityCommand
        verify(streamEventPublisher, never()).publishTargetUpdate(eq("Project"), eq(0L), any());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Wires the factory mocks for a successful command execution that returns
     * the given {@code result} DTO (may be null). Returns the stubbed command.
     */
    private Command stubCommand(String commandType, Object result) throws Exception {
        Command cmd = mock(Command.class);
        doReturn(Void.class).when(apiCommandFactory).getInputType(commandType);
        when(apiCommandFactory.newCommand(eq(commandType), any(), any())).thenReturn(cmd);
        when(commandHandler.execute(cmd)).thenReturn(cmd);
        when(apiCommandFactory.extractResult(eq(commandType), any())).thenReturn(result);
        return cmd;
    }

    /**
     * Wires the factory mocks so that {@code commandHandler.execute()} throws
     * the given exception, simulating a command failure.
     */
    private void stubCommandThrows(String commandType, Exception ex) throws Exception {
        Command cmd = mock(Command.class);
        doReturn(Void.class).when(apiCommandFactory).getInputType(commandType);
        when(apiCommandFactory.newCommand(eq(commandType), any(), any())).thenReturn(cmd);
        when(commandHandler.execute(cmd)).thenThrow(ex);
    }
}
