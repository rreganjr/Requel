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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.command.CommandMetadata;
import com.rreganjr.command.CommandMetadataAware;
import com.rreganjr.repository.jpa.BeanValidationException;
import com.rreganjr.requel.service.api.dto.EditGoalInput;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

/**
 * Covers where issue #171 puts DTO validation in the dispatch sequence.
 *
 * <p>The handler exists rather than validating in {@code ApiCommandFactory.newCommand} so that
 * {@code AuthorizingCommandHandler}, which wraps it, runs first — an unauthorized caller must get a
 * 403 without learning the input's field names. That ordering is pinned end-to-end by
 * {@code AuthorizationIT.noAccessWithInvalidPayloadStillReturnsForbidden}; this class covers the
 * handler's own contract, including the two skip cases that must not silently swallow a command.
 */
@DisplayName("ValidatingCommandHandler — validates the input DTO inside the handler chain")
class ValidatingCommandHandlerTest {

    private static ValidatorFactory factory;

    private RecordingHandler delegate;
    private ValidatingCommandHandler handler;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    @BeforeEach
    void setUp() {
        delegate = new RecordingHandler();
        handler = new ValidatingCommandHandler(delegate,
                new CommandInputValidator(factory.getValidator()));
    }

    @Test
    @DisplayName("a satisfied input DTO passes through to the delegate")
    void validInputReachesDelegate() throws Exception {
        var command = new MetadataCarryingCommand();
        command.setCommandMetadata(new CommandMetadata("EditGoal",
                new EditGoalInput("Project A", null, "A goal", null, null)));

        Command returned = handler.execute(command);

        assertTrue(delegate.executed, "the delegate must run for a valid input");
        assertSame(command, returned, "the handler must return what the delegate returned");
    }

    @Test
    @DisplayName("a violated input DTO throws and the delegate never runs")
    void invalidInputShortCircuits() {
        var command = new MetadataCarryingCommand();
        command.setCommandMetadata(new CommandMetadata("EditGoal",
                new EditGoalInput("Project A", null, "  ", null, null)));

        BeanValidationException e =
                assertThrows(BeanValidationException.class, () -> handler.execute(command));

        assertArrayEquals(new String[] {"name"}, e.getEntityPropertyNames());
        assertFalse(delegate.executed, "an invalid command must not reach the delegate");
    }

    @Test
    @DisplayName("a command with no metadata is passed through, not rejected")
    void noMetadataIsSkipped() throws Exception {
        // Sub-commands a command runs internally (the detach cascade inside a delete, the assistant
        // applicators) are constructed directly in Java and never had an input DTO. They must not be
        // blocked here. ApiCommandFactory fails fast if an API command cannot carry metadata, so
        // this skip cannot hide a command that should have been validated.
        var command = new MetadataCarryingCommand();

        handler.execute(command);

        assertTrue(delegate.executed, "a command with no metadata must still execute");
    }

    @Test
    @DisplayName("a command whose metadata carries a null input is passed through")
    void nullInputIsSkipped() throws Exception {
        var command = new MetadataCarryingCommand();
        command.setCommandMetadata(new CommandMetadata("SomeCommandWithNoInput", null));

        handler.execute(command);

        assertTrue(delegate.executed, "commands with no input bind to null and must still execute");
    }

    @Test
    @DisplayName("a command that is not CommandMetadataAware is passed through")
    void nonMetadataAwareCommandIsSkipped() throws Exception {
        Command command = () -> { /* no-op */ };

        handler.execute(command);

        assertTrue(delegate.executed, "a plain Command must still execute");
    }

    /** Minimal stand-in for the AbstractCommand subclasses the API dispatches. */
    private static class MetadataCarryingCommand implements Command, CommandMetadataAware {
        private CommandMetadata metadata;

        @Override
        public void execute() {
            // the handler chain calls execute() on the delegate's terms, not here
        }

        @Override
        public CommandMetadata getCommandMetadata() {
            return metadata;
        }

        @Override
        public void setCommandMetadata(CommandMetadata metadata) {
            this.metadata = metadata;
        }
    }

    private static class RecordingHandler implements CommandHandler {
        private boolean executed;

        @Override
        public <T extends Command> T execute(T command) {
            executed = true;
            return command;
        }
    }
}
