package com.rreganjr.requel.service.command;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command handler decorator that resolves the current authenticated user from
 * Spring Security and sets {@code editedBy} on commands that support it.
 * This eliminates the need for callers to manually set editedBy before dispatch.
 *
 * <p>Wraps the outermost handler in the chain so editedBy is set before
 * authorization checks or retries.</p>
 *
 * <p>If no SecurityContext is present (e.g., during tests or background jobs),
 * the handler silently skips user resolution and delegates directly.</p>
 */
public class CurrentUserCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserCommandHandler.class);

    private final CommandHandler delegate;
    private final CurrentUserResolver currentUserResolver;

    public CurrentUserCommandHandler(CommandHandler delegate, CurrentUserResolver currentUserResolver) {
        this.delegate = delegate;
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public <T extends Command> T execute(T command) throws Exception {
        // Only resolve the user when the command actually needs it
        if (command instanceof EditCommand editCmd && editCmd.getEditedBy() == null) {
            resolveAndSet(user -> editCmd.setEditedBy(user));
        } else if (command instanceof EditUserCommand userCmd) {
            // EditUserCommand doesn't extend EditCommand and has no getEditedBy() on the interface,
            // so always set it — the command impl handles idempotency
            resolveAndSet(user -> userCmd.setEditedBy(user));
        }
        return delegate.execute(command);
    }

    private void resolveAndSet(java.util.function.Consumer<com.rreganjr.requel.user.User> setter) {
        try {
            setter.accept(currentUserResolver.resolve());
        } catch (Exception e) {
            // No SecurityContext, anonymous user, or user not found — skip
            log.debug("Could not resolve current user for editedBy injection: {}", e.getMessage());
        }
    }
}
