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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.RecordComponent;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.rreganjr.repository.jpa.BeanValidationException;
import com.rreganjr.requel.service.api.dto.EditActorInput;
import com.rreganjr.requel.service.api.dto.EditGlossaryTermInput;
import com.rreganjr.requel.service.api.dto.EditGoalInput;
import com.rreganjr.requel.service.api.dto.EditNonUserStakeholderInput;
import com.rreganjr.requel.service.api.dto.EditReportGeneratorInput;
import com.rreganjr.requel.service.api.dto.EditScenarioInput;
import com.rreganjr.requel.service.api.dto.EditStoryInput;
import com.rreganjr.requel.service.api.dto.EditUseCaseInput;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

/**
 * Issue #331: on every Edit input in #331's table, {@code name} is required when creating (the id
 * is null) and optional when editing, where null keeps the current name and {@code ""} is still
 * refused. Each record is built reflectively with every other component left null, so a new
 * required component on one of them would show up here as an unexpected violation.
 */
class RequiredOnCreateTest {

    private static ValidatorFactory factory;
    private static CommandInputValidator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = new CommandInputValidator(factory.getValidator());
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    static Stream<Arguments> inputs() {
        return Stream.of(
                Arguments.of(EditGoalInput.class, "goalId"),
                Arguments.of(EditActorInput.class, "actorId"),
                Arguments.of(EditUseCaseInput.class, "useCaseId"),
                Arguments.of(EditScenarioInput.class, "scenarioId"),
                Arguments.of(EditGlossaryTermInput.class, "termId"),
                Arguments.of(EditNonUserStakeholderInput.class, "stakeholderId"),
                Arguments.of(EditReportGeneratorInput.class, "reportId"),
                Arguments.of(EditStoryInput.class, "storyId"));
    }

    @ParameterizedTest(name = "{0}: create without a name is refused on name")
    @MethodSource("inputs")
    void createWithoutANameIsRefused(Class<? extends Record> type, String idField) throws Exception {
        assertRefusedOnName(build(type, idField, null, null));
    }

    @ParameterizedTest(name = "{0}: create with a blank name is refused on name")
    @MethodSource("inputs")
    void createWithABlankNameIsRefused(Class<? extends Record> type, String idField) throws Exception {
        assertRefusedOnName(build(type, idField, null, "  "));
    }

    @ParameterizedTest(name = "{0}: create with a name passes")
    @MethodSource("inputs")
    void createWithANamePasses(Class<? extends Record> type, String idField) throws Exception {
        Record input = build(type, idField, null, "A name");
        assertDoesNotThrow(() -> validator.validate(input));
    }

    @ParameterizedTest(name = "{0}: update without a name passes (keeps the current name)")
    @MethodSource("inputs")
    void updateWithoutANamePasses(Class<? extends Record> type, String idField) throws Exception {
        Record input = build(type, idField, 7L, null);
        assertDoesNotThrow(() -> validator.validate(input));
    }

    @ParameterizedTest(name = "{0}: update with an empty name is refused on name")
    @MethodSource("inputs")
    void updateWithAnEmptyNameIsRefused(Class<? extends Record> type, String idField) throws Exception {
        assertRefusedOnName(build(type, idField, 7L, ""));
    }

    private static void assertRefusedOnName(Record input) {
        BeanValidationException e =
                assertThrows(BeanValidationException.class, () -> validator.validate(input));
        assertArrayEquals(new String[] { "name" }, e.getEntityPropertyNames(),
                input.getClass().getSimpleName());
    }

    /** projectName "P", the id and name as given, every other component null (or false). */
    private static Record build(Class<? extends Record> type, String idField, Long id, String name)
            throws Exception {
        RecordComponent[] components = type.getRecordComponents();
        Class<?>[] types = new Class<?>[components.length];
        Object[] args = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            RecordComponent c = components[i];
            types[i] = c.getType();
            args[i] = switch (c.getName()) {
                case "projectName" -> "P";
                case "name" -> name;
                default -> c.getName().equals(idField) ? id
                        : c.getType() == boolean.class ? Boolean.FALSE : null;
            };
        }
        return type.getDeclaredConstructor(types).newInstance(args);
    }
}
