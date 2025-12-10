/*
 * $Id: $
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
 *
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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
package com.rreganjr.validator;

public class InvalidStateException extends RuntimeException {
    private final InvalidValue[] invalidValues;

    public InvalidStateException(String message, InvalidValue[] invalidValues) {
        super(message);
        this.invalidValues = invalidValues != null ? invalidValues : new InvalidValue[0];
    }

    public InvalidStateException(Throwable cause, InvalidValue[] invalidValues) {
        super(cause);
        this.invalidValues = invalidValues != null ? invalidValues : new InvalidValue[0];
    }

    public InvalidStateException(String message) { this(message, null); }
    public InvalidStateException(Throwable cause) { this(cause, null); }

    public InvalidValue[] getInvalidValues() { return invalidValues; }
}

