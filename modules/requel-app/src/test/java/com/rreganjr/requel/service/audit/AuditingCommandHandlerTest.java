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
package com.rreganjr.requel.service.audit;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandMetadata;
import com.rreganjr.command.CommandMetadataAware;
import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.user.command.EditUserCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuditingCommandHandler}.
 *
 * The handler is a decorator constructed directly (not a Spring bean), so these
 * are pure Mockito unit tests — no Spring context required.
 *
 * Scenarios covered:
 * - Background/NLP commands (no {@code CommandMetadataAware}) are skipped
 * - Commands with null metadata are skipped
 * - API commands write an audit row with correct userId, commandType, commandClass
 * - projectId is resolved via projectName on the input record (strategy 1)
 * - projectId is resolved via ProjectScopedCommand entity (strategy 2 fallback)
 * - Sensitive fields in the payload are redacted
 * - Delegate exceptions are re-thrown and no audit row is written
 * - Audit save failures do not propagate — the command result is still returned
 */
@ExtendWith(MockitoExtension.class)
class AuditingCommandHandlerTest {

    @Mock com.rreganjr.command.CommandHandler delegate;
    @Mock CommandAuditLogRepository auditLogRepository;
    @Mock SensitiveFieldRedactor redactor;
    @Mock ProjectRepository projectRepository;

    AuditingCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AuditingCommandHandler(delegate, auditLogRepository, redactor, projectRepository);
    }

    // -------------------------------------------------------------------------
    // Skipping rules
    // -------------------------------------------------------------------------

    @Test
    void backgroundCommandWithoutMetadataIsNotAudited() throws Exception {
        // A plain Command (NLP analysis, background task) has no CommandMetadataAware
        BackgroundCommand cmd = new BackgroundCommand();
        when(delegate.execute(cmd)).thenReturn(cmd);

        handler.execute(cmd);

        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void commandWithNullMetadataIsNotAudited() throws Exception {
        // CommandMetadataAware but getCommandMetadata() returns null
        ApiEditCommand cmd = new ApiEditCommand(null, mock(User.class));
        when(delegate.execute(cmd)).thenReturn(cmd);

        handler.execute(cmd);

        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void commandWithMetadataButNoUserIsNotAudited() throws Exception {
        // metadata present, but editedBy is null — userId can't be resolved
        ApiEditCommand cmd = new ApiEditCommand(new CommandMetadata("EditGoal", null), null);
        when(delegate.execute(cmd)).thenReturn(cmd);

        handler.execute(cmd);

        verifyNoInteractions(auditLogRepository);
    }

    // -------------------------------------------------------------------------
    // Audit row written for API commands
    // -------------------------------------------------------------------------

    @Test
    void apiCommandWritesAuditRow() throws Exception {
        User user = mockUser(42L);
        record GoalInput(String name) {}
        CommandMetadata meta = new CommandMetadata("EditGoal", new GoalInput("My Goal"));
        ApiEditCommand cmd = new ApiEditCommand(meta, user);
        when(delegate.execute(cmd)).thenReturn(cmd);
        when(redactor.redact(any())).thenReturn("{\"name\":\"My Goal\"}");

        handler.execute(cmd);

        ArgumentCaptor<CommandAuditLog> captor = ArgumentCaptor.forClass(CommandAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        CommandAuditLog entry = captor.getValue();
        assertEquals(42L, entry.getUserId(), "userId should match editedBy.getId()");
        assertEquals("EditGoal", entry.getCommandType(), "commandType should come from metadata");
        assertTrue(entry.getCommandClass().contains("ApiEditCommand"),
                "commandClass should reflect the command impl");
        assertNotNull(entry.getExecutedAt(), "executedAt should be populated");
    }

    @Test
    void editUserCommandWritesAuditRow() throws Exception {
        // EditUserCommand is a separate interface hierarchy from EditCommand
        com.rreganjr.requel.user.User requeUser = mockRequelUser(7L);
        record UserInput(String username) {}
        CommandMetadata meta = new CommandMetadata("EditUser", new UserInput("newuser"));
        ApiEditUserCommand cmd = new ApiEditUserCommand(meta, requeUser);
        when(delegate.execute(cmd)).thenReturn(cmd);
        when(redactor.redact(any())).thenReturn("{\"username\":\"newuser\"}");

        handler.execute(cmd);

        ArgumentCaptor<CommandAuditLog> captor = ArgumentCaptor.forClass(CommandAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
        assertEquals("EditUser", captor.getValue().getCommandType());
    }

    // -------------------------------------------------------------------------
    // projectId resolution
    // -------------------------------------------------------------------------

    @Test
    void projectIdResolvedFromProjectNameOnInput() throws Exception {
        // Strategy 1: input record has a projectName() accessor
        User user = mockUser(1L);
        record ProjectNameInput(String projectName, String name) {}
        CommandMetadata meta = new CommandMetadata("EditGoal",
                new ProjectNameInput("MyProject", "Goal A"));
        ApiEditCommand cmd = new ApiEditCommand(meta, user);
        when(delegate.execute(cmd)).thenReturn(cmd);
        when(redactor.redact(any())).thenReturn("{}");

        Project project = mock(Project.class);
        when(project.getId()).thenReturn(99L);
        when(projectRepository.findProjectByName("MyProject")).thenReturn(project);

        handler.execute(cmd);

        ArgumentCaptor<CommandAuditLog> captor = ArgumentCaptor.forClass(CommandAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(99L, captor.getValue().getProjectId(),
                "projectId should be resolved via projectName on input");
    }

    @Test
    void projectIdResolvedFromProjectScopedCommandFallback() throws Exception {
        // Strategy 2: no projectName on input, but command implements ProjectScopedCommand
        User user = mockUser(1L);
        record SimpleInput(String name) {}
        CommandMetadata meta = new CommandMetadata("EditGoal", new SimpleInput("Goal B"));

        Project project = mock(Project.class);
        when(project.getId()).thenReturn(55L);
        ProjectScopedApiCommand cmd = new ProjectScopedApiCommand(meta, user, project);
        when(delegate.execute(cmd)).thenReturn(cmd);
        when(redactor.redact(any())).thenReturn("{}");

        handler.execute(cmd);

        ArgumentCaptor<CommandAuditLog> captor = ArgumentCaptor.forClass(CommandAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(55L, captor.getValue().getProjectId(),
                "projectId should fall back to ProjectScopedCommand.getProject().getId()");
    }

    @Test
    void projectIdIsNullWhenNeitherStrategyApplies() throws Exception {
        // Input has no projectName(), command is not ProjectScopedCommand
        User user = mockUser(1L);
        record SimpleInput(String name) {}
        CommandMetadata meta = new CommandMetadata("EditUser", new SimpleInput("x"));
        ApiEditCommand cmd = new ApiEditCommand(meta, user);
        when(delegate.execute(cmd)).thenReturn(cmd);
        when(redactor.redact(any())).thenReturn("{}");

        handler.execute(cmd);

        ArgumentCaptor<CommandAuditLog> captor = ArgumentCaptor.forClass(CommandAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertNull(captor.getValue().getProjectId(),
                "projectId should be null when neither resolution strategy applies");
    }

    // -------------------------------------------------------------------------
    // Sensitive field redaction
    // -------------------------------------------------------------------------

    @Test
    void sensitiveFieldsAreRedactedInPayload() throws Exception {
        User user = mockUser(1L);
        record LoginInput(String username, String password) {}
        CommandMetadata meta = new CommandMetadata("EditUser",
                new LoginInput("alice", "s3cr3t"));
        ApiEditCommand cmd = new ApiEditCommand(meta, user);
        when(delegate.execute(cmd)).thenReturn(cmd);
        when(redactor.redact(any())).thenReturn("{\"username\":\"alice\",\"password\":\"[REDACTED]\"}");

        handler.execute(cmd);

        ArgumentCaptor<CommandAuditLog> captor = ArgumentCaptor.forClass(CommandAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertTrue(captor.getValue().getRequestPayload().contains("[REDACTED]"),
                "password should be redacted in the stored payload");
        assertFalse(captor.getValue().getRequestPayload().contains("s3cr3t"),
                "raw password must not appear in the stored payload");
    }

    // -------------------------------------------------------------------------
    // Resilience
    // -------------------------------------------------------------------------

    @Test
    void delegateExceptionIsRethrownWithoutAudit() throws Exception {
        BackgroundCommand cmd = new BackgroundCommand();
        when(delegate.execute(cmd)).thenThrow(new RuntimeException("command failed"));

        assertThrows(RuntimeException.class, () -> handler.execute(cmd),
                "exception from delegate should propagate");
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void auditSaveFailureDoesNotPropagateCommandResultIsReturned() throws Exception {
        User user = mockUser(1L);
        CommandMetadata meta = new CommandMetadata("EditGoal", null);
        ApiEditCommand cmd = new ApiEditCommand(meta, user);
        when(delegate.execute(cmd)).thenReturn(cmd);
        when(redactor.redact(any())).thenReturn("{}");
        doThrow(new RuntimeException("DB unavailable")).when(auditLogRepository).save(any());

        // Should not throw — audit failure is swallowed; command result returned
        Command result = handler.execute(cmd);

        assertSame(cmd, result, "command result should be returned even if audit save fails");
    }

    // -------------------------------------------------------------------------
    // Test doubles
    // -------------------------------------------------------------------------

    /** Simulates a background/NLP command that is NOT CommandMetadataAware. */
    private static class BackgroundCommand implements Command {
        @Override public void execute() {}
    }

    /** Simulates an API-dispatched project/annotation command. */
    private static class ApiEditCommand implements Command, CommandMetadataAware, EditCommand {
        private CommandMetadata metadata;
        private final User editedBy;

        ApiEditCommand(CommandMetadata metadata, User editedBy) {
            this.metadata = metadata;
            this.editedBy = editedBy;
        }

        @Override public void execute() {}
        @Override public CommandMetadata getCommandMetadata() { return metadata; }
        @Override public void setCommandMetadata(CommandMetadata m) { this.metadata = m; }
        @Override public User getEditedBy() { return editedBy; }
        @Override public void setEditedBy(User u) { throw new UnsupportedOperationException(); }
    }

    /**
     * Simulates an API-dispatched command that also implements {@link ProjectScopedCommand},
     * used for the strategy-2 projectId fallback test.
     */
    private static class ProjectScopedApiCommand extends ApiEditCommand
            implements ProjectScopedCommand {
        private final Project project;

        ProjectScopedApiCommand(CommandMetadata metadata, User editedBy, Project project) {
            super(metadata, editedBy);
            this.project = project;
        }

        @Override public Project getProject() { return project; }
    }

    /** Simulates an API-dispatched user command (EditUserCommand hierarchy). */
    private static class ApiEditUserCommand implements Command, CommandMetadataAware, EditUserCommand {
        private CommandMetadata metadata;
        private final com.rreganjr.requel.user.User editedBy;

        ApiEditUserCommand(CommandMetadata metadata, com.rreganjr.requel.user.User editedBy) {
            this.metadata = metadata;
            this.editedBy = editedBy;
        }

        @Override public void execute() {}
        @Override public CommandMetadata getCommandMetadata() { return metadata; }
        @Override public void setCommandMetadata(CommandMetadata m) { this.metadata = m; }
        @Override public com.rreganjr.requel.user.User getEditedBy() { return editedBy; }
        @Override public void setEditedBy(com.rreganjr.requel.user.User u) { throw new UnsupportedOperationException(); }

        // EditUserCommand fields not relevant to auditing tests
        @Override public com.rreganjr.requel.user.User getUser() { return null; }
        @Override public void setUser(com.rreganjr.requel.user.User u) {}
        @Override public void setUsername(String s) {}
        @Override public void setPassword(String s) {}
        @Override public void setRepassword(String s) {}
        @Override public void setName(String s) {}
        @Override public void setEmailAddress(String s) {}
        @Override public void setPhoneNumber(String s) {}
        @Override public void setOrganizationName(String s) {}
        @Override public void setEditable(Boolean b) {}
        @Override public void setUserRoleNames(java.util.Set<String> s) {}
        @Override public void addUserRoleName(String s) {}
        @Override public void setUserRolePermissionNames(java.util.Map<String, java.util.Set<String>> m) {}
        @Override public void addUserRolePermissionName(String role, String perm) {}
        @Override public void setExpectedVersion(Integer expectedVersion) {}
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Creates a mock platform.identity.User with the given ID. */
    private User mockUser(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    /** Creates a mock requel User (for EditUserCommand) with the given ID. */
    private com.rreganjr.requel.user.User mockRequelUser(Long id) {
        com.rreganjr.requel.user.User user = mock(com.rreganjr.requel.user.User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }
}
