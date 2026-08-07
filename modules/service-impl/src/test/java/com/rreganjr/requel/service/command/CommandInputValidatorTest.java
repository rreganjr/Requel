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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rreganjr.repository.jpa.BeanValidationException;
import com.rreganjr.requel.service.api.dto.EditGoalInput;
import com.rreganjr.requel.service.api.dto.EditUserInput;
import com.rreganjr.validator.ValidationLimits;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

/**
 * Covers the enforcement half of issue #171: before it, the constraints on the {@code Edit*Input}
 * records were metadata only and nothing in the codebase called a {@code Validator}.
 *
 * <p>The assertions that matter are the ones about the exception's <em>shape</em>, not just that it
 * throws. {@code CommandController} reads {@code getEntityPropertyNames()} and
 * {@code getFieldMessages()} as parallel arrays to build {@code CommandResult.FieldViolation}s, and
 * the Angular {@code applyCommandErrors} adapter resolves those field names against form controls.
 * A mismatched pair or a non-deterministic order would surface as a violation landing on the wrong
 * field, or on none at all.
 */
@DisplayName("CommandInputValidator — bean validation of command input DTOs")
class CommandInputValidatorTest {

    private static ValidatorFactory factory;
    private static CommandInputValidator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = new CommandInputValidator(factory.getValidator());
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    @DisplayName("null input is allowed — commands with no input bind to null")
    void nullInputPasses() {
        assertDoesNotThrow(() -> validator.validate(null));
    }

    @Test
    @DisplayName("a satisfied DTO passes untouched")
    void validInputPasses() {
        var input = new EditGoalInput("Project A", 1L, "Reduce onboarding time", "some text", 3);
        assertDoesNotThrow(() -> validator.validate(input));
    }

    @Test
    @DisplayName("optional fields may be null — only the annotated ones are required")
    void nullOptionalFieldsPass() {
        var input = new EditGoalInput("Project A", null, "A goal", null, null);
        assertDoesNotThrow(() -> validator.validate(input));
    }

    @Test
    @DisplayName("a blank @NotBlank field throws with the DTO field name, not an entity property")
    void blankNameIsRejected() {
        var input = new EditGoalInput("Project A", null, "   ", null, null);

        BeanValidationException e =
                assertThrows(BeanValidationException.class, () -> validator.validate(input));

        assertArrayEquals(new String[] {"name"}, e.getEntityPropertyNames());
        assertEquals(1, e.getFieldMessages().length);
        assertTrue(e.getMessage().contains("name"),
                () -> "combined message should name the field: " + e.getMessage());
    }

    @Test
    @DisplayName("property names and field messages stay parallel and in a deterministic order")
    void multipleViolationsAreOrderedAndParallel() {
        var input = new EditGoalInput("", null, "", null, null);

        BeanValidationException first =
                assertThrows(BeanValidationException.class, () -> validator.validate(input));
        BeanValidationException second =
                assertThrows(BeanValidationException.class, () -> validator.validate(input));

        // Sorted by property path, so `name` precedes `projectName` regardless of the order the
        // Validator's unordered Set happened to yield.
        assertArrayEquals(new String[] {"name", "projectName"}, first.getEntityPropertyNames());
        assertEquals(first.getEntityPropertyNames().length, first.getFieldMessages().length,
                "CommandController reads these as parallel arrays");

        // Same input twice must give the same order — this is the flake this sort prevents.
        assertArrayEquals(first.getEntityPropertyNames(), second.getEntityPropertyNames());
        assertArrayEquals(first.getFieldMessages(), second.getFieldMessages());
    }

    @Test
    @DisplayName("the rejected input is carried on the exception for logging and auditing")
    void rejectedInputIsCarried() {
        var input = new EditGoalInput("Project A", null, "", null, null);

        BeanValidationException e =
                assertThrows(BeanValidationException.class, () -> validator.validate(input));

        assertSame(input, e.getEntity());
        assertEquals(EditGoalInput.class, e.getEntityType());
    }

    // ---------------------------------------------------------------- slice 2: @Size and @Email

    @Test
    @DisplayName("a name at exactly the limit is accepted")
    void nameAtTheLimitPasses() {
        var input = new EditGoalInput("Project A", null,
                "a".repeat(ValidationLimits.ARTIFACT_NAME_MAX), null, null);
        assertDoesNotThrow(() -> validator.validate(input));
    }

    @Test
    @DisplayName("a name one character over the limit is rejected with the shared message")
    void nameOverTheLimitIsRejected() {
        var input = new EditGoalInput("Project A", null,
                "a".repeat(ValidationLimits.ARTIFACT_NAME_MAX + 1), null, null);

        BeanValidationException e =
                assertThrows(BeanValidationException.class, () -> validator.validate(input));

        assertArrayEquals(new String[] {"name"}, e.getEntityPropertyNames());
        assertEquals("must be " + ValidationLimits.ARTIFACT_NAME_MAX + " characters or fewer.",
                e.getFieldMessages()[0]);
    }

    @Test
    @DisplayName("artifact text is deliberately unbounded")
    void textIsNotLengthLimited() {
        // AbstractTextEntity.getText() is @Lob/longtext, so there is no server-side bound to mirror.
        // If a @Size ever appears on text, validation-limits.ts needs the matching entry or the
        // client will accept what the server rejects.
        var input = new EditGoalInput("Project A", null, "A goal",
                "x".repeat(ValidationLimits.ARTIFACT_NAME_MAX * 40), null);
        assertDoesNotThrow(() -> validator.validate(input));
    }

    @Test
    @DisplayName("a malformed email address is rejected on the field the client binds")
    void malformedEmailIsRejected() {
        var input = new EditUserInput(1L, 1, "ron", null, null, "Ron", "not-an-email",
                null, null, true, Set.of("Administrator"), Map.of());

        BeanValidationException e =
                assertThrows(BeanValidationException.class, () -> validator.validate(input));

        // `emailAddress`, not `email` -- this is the DTO field name the Angular applyCommandErrors
        // adapter resolves against the form control, and the reason #176 becomes unnecessary.
        assertArrayEquals(new String[] {"emailAddress"}, e.getEntityPropertyNames());
    }

    @Test
    @DisplayName("a well-formed email address passes")
    void wellFormedEmailPasses() {
        var input = new EditUserInput(1L, 1, "ron", null, null, "Ron", "ron@example.com",
                null, null, true, Set.of("Administrator"), Map.of());
        assertDoesNotThrow(() -> validator.validate(input));
    }
}

