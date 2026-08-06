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
package com.rreganjr.requel.service.command;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.command.CommandMetadata;
import com.rreganjr.command.CommandMetadataAware;

/**
 * Command handler that bean-validates the input DTO an API-dispatched command was built from
 * (issue #171).
 *
 * <p><strong>Position in the chain matters and is the reason this is a handler at all.</strong> It
 * sits <em>inside</em> {@code AuthorizingCommandHandler}:
 *
 * <pre>
 * Auditing -&gt; CurrentUser -&gt; RetryOnLockFailures -&gt; ExceptionMapping
 *          -&gt; Authorizing -&gt; Validating -&gt; FindingResolutionTracking -&gt; AnalysisInvoking -&gt; Default
 * </pre>
 *
 * so a caller who is not permitted to run the command gets a 403 and learns nothing about the
 * shape of its input. Validating in {@code ApiCommandFactory.newCommand} instead — which is where
 * this started — put validation <em>before</em> the chain and therefore before authorization, so an
 * unauthorized caller sending a malformed payload got a 422 listing field names and messages.
 *
 * <p><strong>How it gets the input.</strong> The chain carries a {@link Command}, not the DTO, so
 * the input is read back off the command via {@link CommandMetadata}, which
 * {@code ApiCommandFactory.newCommand} stamps on every command it builds. A command with no
 * metadata, or with a null input, is skipped: that is the correct behavior for the sub-commands a
 * command runs internally (the detach cascade inside a delete, the assistant applicators) since
 * those are constructed directly in Java and never had an input DTO to validate.
 * {@code ApiCommandFactory} fails fast if an API command with an input cannot carry metadata, so
 * this skip can never silently swallow a command that should have been validated.
 *
 * <p>The {@code BeanValidationException} thrown here reaches the client intact: it is an
 * {@code EntityValidationException}, so {@code RetryOnLockFailuresCommandHandler} rethrows it
 * without retrying and {@code ExceptionMapper.convertException} returns it unchanged rather than
 * re-wrapping it, preserving the per-field messages {@code CommandController} needs.
 */
public class ValidatingCommandHandler implements CommandHandler {

    private final CommandHandler delegate;
    private final CommandInputValidator inputValidator;

    public ValidatingCommandHandler(CommandHandler delegate, CommandInputValidator inputValidator) {
        this.delegate = delegate;
        this.inputValidator = inputValidator;
    }

    @Override
    public <T extends Command> T execute(T command) throws Exception {
        if (command instanceof CommandMetadataAware aware) {
            CommandMetadata metadata = aware.getCommandMetadata();
            if (metadata != null) {
                // null input is a no-op in the validator; commands with no input bind to null.
                inputValidator.validate(metadata.getInput());
            }
        }
        return delegate.execute(command);
    }
}
