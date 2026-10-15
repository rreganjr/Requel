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
package com.rreganjr.requel;

/**
 * A value in a small closed vocabulary that carries a caller-facing meaning, so the meaning is
 * declared once, next to the value, and every consumer (a UI picker, a gateway input schema, a
 * validation message) reads it from there instead of keeping its own copy. Issue #257.
 */
public interface DescribedValue {

	/**
	 * @return one sentence saying what the value means, written for the person or agent choosing
	 *         it.
	 */
	String getDescription();
}
