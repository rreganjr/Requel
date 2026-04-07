/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
import com.rreganjr.command.CommandHandler;
import com.rreganjr.command.CommandMetadata;
import com.rreganjr.command.CommandMetadataAware;
import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.command.EditProjectOrDomainEntityCommand;
import com.rreganjr.requel.user.command.EditUserCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import java.lang.reflect.Method;
import java.time.Instant;

/**
 * Outermost command handler decorator that logs successful command executions
 * to the {@code command_audit_log} table.
 * <p>
 * Runs after the inner chain commits. The repository's own transaction handles
 * the audit insert independently — a logging failure cannot roll back the command.
 * <p>
 * On exception the handler re-throws without logging — only successful
 * commands are audited.
 */
public class AuditingCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(AuditingCommandHandler.class);

    private final CommandHandler delegate;
    private final CommandAuditLogRepository auditLogRepository;
    private final SensitiveFieldRedactor redactor;
    private final ProjectRepository projectRepository;

    public AuditingCommandHandler(CommandHandler delegate,
                                  CommandAuditLogRepository auditLogRepository,
                                  SensitiveFieldRedactor redactor,
                                  ProjectRepository projectRepository) {
        this.delegate = delegate;
        this.auditLogRepository = auditLogRepository;
        this.redactor = redactor;
        this.projectRepository = projectRepository;
    }

    @Override
    public <T extends Command> T execute(T command) throws Exception {
        T result = delegate.execute(command);
        try {
            persistAuditEntry(result);
        } catch (Exception e) {
            log.warn("Failed to write audit log for command {}: {}",
                    result.getClass().getSimpleName(), e.getMessage(), e);
        }
        return result;
    }

    private void persistAuditEntry(Command command) {
        // Only audit API-dispatched commands — background/NLP commands have no metadata
        if (!(command instanceof CommandMetadataAware cma) || cma.getCommandMetadata() == null) {
            return;
        }
        CommandMetadata metadata = cma.getCommandMetadata();

        Long userId = resolveUserId(command);
        if (userId == null) {
            log.info("Skipping audit log — no user ID available for {}", command.getClass().getName());
            return;
        }

        String commandType = metadata.getCommandType() != null
                ? metadata.getCommandType()
                : command.getClass().getSimpleName();
        String redactedPayload = redactor.redact(metadata.getInput());

        // Unwrap CGLIB proxy to get the real command class name
        String commandClass = ClassUtils.getUserClass(command).getName();

        Long projectId = resolveProjectId(command, metadata);

        CommandAuditLog entry = new CommandAuditLog(
                userId,
                Instant.now(),
                commandType,
                commandClass,
                projectId,
                redactedPayload
        );
        auditLogRepository.save(entry);
        log.info("Audit log: user={} command={} project={}", userId, commandType, projectId);
    }

    /**
     * Resolve the user ID from the command. Handles two separate interface
     * hierarchies:
     * <ul>
     *   <li>{@link EditCommand} — project/annotation commands</li>
     *   <li>{@link EditUserCommand} — user commands (different editedBy type, no shared interface)</li>
     * </ul>
     */
    private Long resolveUserId(Command command) {
        if (command instanceof EditCommand editCmd && editCmd.getEditedBy() != null) {
            return editCmd.getEditedBy().getId();
        }
        if (command instanceof EditUserCommand userCmd && userCmd.getEditedBy() != null) {
            return userCmd.getEditedBy().getId();
        }
        return null;
    }

    /**
     * Extract the project ID for an API-dispatched command.
     * <p>
     * Tries two strategies in order:
     * <ol>
     *   <li>Reflect on the metadata input record for a {@code projectName()} accessor and
     *       look up the project by name — covers all project commands whose input records
     *       carry a {@code projectName} field (the common case)</li>
     *   <li>{@link ProjectScopedCommand#getProject()} or
     *       {@link EditProjectOrDomainEntityCommand#getProjectOrDomain()} — fallback for
     *       commands whose input does not carry projectName (e.g. EditProjectCommand uses
     *       ProjectScopedCommand)</li>
     * </ol>
     */
    private Long resolveProjectId(Command command, CommandMetadata metadata) {
        // Strategy 1: projectName on the typed input record (fastest — one field lookup)
        Object input = metadata.getInput();
        if (input != null) {
            try {
                // Java records expose fields via accessor with the field name (no "get" prefix)
                Method accessor = ReflectionUtils.findMethod(input.getClass(), "projectName");
                if (accessor != null) {
                    ReflectionUtils.makeAccessible(accessor);
                    Object name = accessor.invoke(input);
                    if (name instanceof String projectName && !projectName.isBlank()) {
                        return projectRepository.findProjectByName(projectName).getId();
                    }
                }
            } catch (Exception e) {
                log.debug("Could not resolve project ID from input projectName: {}", e.getMessage());
            }
        }

        // Strategy 2: domain entity on the command (fallback for commands without projectName input)
        Object entity = null;
        if (command instanceof ProjectScopedCommand psc) {
            entity = psc.getProject();
        } else if (command instanceof EditProjectOrDomainEntityCommand podCmd) {
            entity = podCmd.getProjectOrDomain();
        }
        if (entity != null) {
            try {
                Method getId = ReflectionUtils.findMethod(entity.getClass(), "getId");
                if (getId != null) {
                    ReflectionUtils.makeAccessible(getId);
                    Object id = getId.invoke(entity);
                    if (id instanceof Long longId) {
                        return longId;
                    }
                }
            } catch (Exception e) {
                log.warn("Could not extract project ID from entity: {}", e.getMessage());
            }
        }

        return null;
    }
}
