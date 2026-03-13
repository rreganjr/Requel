package com.rreganjr.requel.service.audit;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.command.CommandMetadata;
import com.rreganjr.command.CommandMetadataAware;
import com.rreganjr.platform.command.EditCommand;
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

    public AuditingCommandHandler(CommandHandler delegate,
                                  CommandAuditLogRepository auditLogRepository,
                                  SensitiveFieldRedactor redactor) {
        this.delegate = delegate;
        this.auditLogRepository = auditLogRepository;
        this.redactor = redactor;
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
        Long userId = resolveUserId(command);
        if (userId == null) {
            log.info("Skipping audit log — no user ID available for {}", command.getClass().getName());
            return;
        }

        // Extract command type and input from metadata
        String commandType = command.getClass().getSimpleName();
        String redactedPayload = null;
        if (command instanceof CommandMetadataAware metadataAware) {
            CommandMetadata metadata = metadataAware.getCommandMetadata();
            if (metadata != null) {
                if (metadata.getCommandType() != null) {
                    commandType = metadata.getCommandType();
                }
                redactedPayload = redactor.redact(metadata.getInput());
            }
        }

        // Unwrap CGLIB proxy to get the real command class name
        String commandClass = ClassUtils.getUserClass(command).getName();

        // Extract project ID via JPA PersistenceUnitUtil (avoids needing public getId())
        Long projectId = resolveProjectId(command);

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
     * Extract the project/domain ID by calling {@code getId()} via reflection.
     * <p>
     * Domain entities may be wrapped in legacy CGLIB proxies ({@code $EnhancerByCGLIB$})
     * that neither {@code Hibernate.unproxy()} nor {@code PersistenceUnitUtil} can handle.
     * Reflection through the proxy works because CGLIB proxies intercept all method calls
     * and delegate to the real target.
     */
    private Long resolveProjectId(Command command) {
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
                log.warn("Could not extract project ID: {}", e.getMessage());
            }
        }
        return null;
    }
}
