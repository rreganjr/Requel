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
package com.rreganjr.requel.service.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Fields required when an Edit input creates an entity and optional when it edits one, per #316's
 * partial-update contract: on update a null or absent field keeps the entity's current value
 * (issue #331). {@code id} names the record component whose value selects the entity to edit; when
 * it is null the input creates.
 *
 * <ul>
 * <li>Create ({@code id} null): each field must not be null or blank.</li>
 * <li>Update ({@code id} set): each field may be null or absent, but {@code ""} is still refused,
 * since these fields cannot be cleared.</li>
 * </ul>
 *
 * A violation is reported on the field itself ({@code name}), not on the record, so field errors
 * still route to the right form control and gateway callers see which field to fix. Because the
 * fields carry no {@code @NotBlank}, the generated MCP/CLI schemas no longer list them as always
 * required.
 */
@Documented
@Constraint(validatedBy = RequiredOnCreateValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredOnCreate {

    /** The record component holding the id of the entity being edited; null means create. */
    String id();

    /** The record components required on create and not clearable on update. */
    String[] fields() default { "name" };

    String message() default "must not be blank";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
