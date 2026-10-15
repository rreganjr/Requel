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
 * Declares that a {@code String} field of a gateway input DTO takes one of the names of an enum,
 * so the published input schema can list them. {@code CommandInputSchema} emits the enum's
 * constant names as the JSON-schema {@code enum}, in declaration order, and when the enum is a
 * {@code com.rreganjr.requel.DescribedValue} it adds a {@code description} giving each value's
 * meaning. The enum stays the only list: nothing restates the values by hand.
 *
 * <p>The field stays a {@code String} so an unknown value reaches the command and becomes a
 * field-level validation error naming the permitted values, rather than a deserialization
 * failure. This annotation does not validate anything itself. Issue #257.
 *
 * @author ron
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.RECORD_COMPONENT, ElementType.METHOD })
public @interface AllowedValues {

    /** The enum whose constant names are the allowed values. */
    Class<? extends Enum<?>> value();
}
