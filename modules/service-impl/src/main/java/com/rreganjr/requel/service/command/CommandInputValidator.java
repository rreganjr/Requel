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

import java.util.Set;

import org.springframework.stereotype.Component;

import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.repository.jpa.BeanValidationException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

/**
 * Bean-validates a command's input DTO. Driven by {@link ValidatingCommandHandler}, which owns the
 * decision of <em>when</em> in the dispatch sequence this runs; this class owns only the mechanics.
 *
 * <p>Until issue #171 nothing did this: the {@code @NotBlank} / {@code @NotNull} / {@code @Size}
 * annotations on the {@code Edit*Input} records in {@code service-api} were metadata only, read
 * reflectively by the MCP schema generator (#104) but never enforced. Validation happened
 * exclusively at Hibernate flush time, where
 * {@link com.rreganjr.repository.jpa.BeanValidationExceptionAdapter} converts the resulting
 * {@link ConstraintViolationException} into a {@link BeanValidationException}. That remains the path
 * for entity-level constraints; this is its counterpart for input DTOs.
 *
 * <p>Violations are reported as a {@link BeanValidationException}, which the existing
 * {@code catch (EntityValidationException)} branches in {@code CommandController} and
 * {@code InProcessCommandGateway} already translate into per-field API errors. So DTO violations
 * reach the client through the same {@code CommandResult.FieldViolation} shape as entity violations,
 * with no client contract change.
 *
 * <p>One consequence worth knowing: because the property paths come from the DTO rather than from a
 * JPA entity, {@code FieldViolation.field} now carries <em>input DTO field names</em>
 * ({@code emailAddress}, {@code userRoleNames}) rather than entity property names for anything that
 * fails here. That is what issue #176 asked for, so the per-editor
 * {@code { entityProperty: controlName }} maps added by #132 are largely redundant.
 */
@Component
public class CommandInputValidator {

    private final Validator validator;

    public CommandInputValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * Validate a bound command input DTO.
     *
     * <p>Ordering of the reported fields is {@link BeanValidationException#of}'s job, not this
     * method's: {@code ConstraintViolationException} copies the violation set it is handed into a
     * {@code HashSet}, so sorting before constructing it would be thrown away.
     *
     * @param input the bound input DTO, or {@code null} for commands that take no input
     * @throws BeanValidationException if any constraint on {@code input} is violated
     */
    public void validate(Object input) {
        if (input == null) {
            return;
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(input);
        if (violations.isEmpty()) {
            return;
        }
        throw BeanValidationException.of(new ConstraintViolationException(violations),
                input.getClass(), input, EntityExceptionActionType.Unknown);
    }
}
