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

/**
 * Declares the JPA entity property an input-DTO field corresponds to, when the two names differ
 * (e.g. {@code password} on the DTO backs {@code encryptedPassword} on the entity). Issue #176.
 *
 * <p>Entity-level bean-validation runs at Hibernate flush, so a violation's property path is the
 * <em>entity</em> property name. Angular forms are built from the input DTOs, so those two
 * vocabularies diverge exactly where it matters. {@code CommandController} reads this annotation off
 * the command's input-DTO class to translate the entity property name back to the DTO field name
 * before building {@code CommandResult.FieldViolation.field}, so the client can route the violation
 * to the right control without a per-editor map. Fields whose names already coincide need no
 * annotation — translation passes them through unchanged.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
public @interface FromEntityProperty {
    /** The JPA entity property name this DTO field maps from. */
    String value();
}
