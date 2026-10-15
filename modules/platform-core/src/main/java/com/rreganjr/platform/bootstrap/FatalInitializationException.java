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
package com.rreganjr.platform.bootstrap;

/**
 * Thrown by a {@link SystemInitializer} whose failure must stop the system coming up, rather than
 * being logged and stepped over.
 * <p>
 * {@link DatabaseInitializer#initialize()} deliberately swallows a {@link RuntimeException} from one
 * initializer so a single failure cannot abort the rest of the chain. That is right for an
 * initializer whose work is optional — but it also means an initializer can fail on every boot
 * forever and leave nothing behind but an ERROR line in a passing build (issue #288). An initializer
 * that has established its work was both asked for and impossible throws this instead, and the
 * chain stops.
 *
 * @author ron
 */
public class FatalInitializationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public FatalInitializationException(String message) {
		super(message);
	}

	public FatalInitializationException(String message, Throwable cause) {
		super(message, cause);
	}
}
