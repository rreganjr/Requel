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

/**
 * Implemented by domain Command classes to accept input from the CQRS API layer.
 * The input type T is a simple DTO (Java record) matching the JSON shape the
 * Angular frontend sends.
 *
 * @param <T> the input DTO type for this command
 */
public interface ApiCommand<T> {

    /**
     * Apply the deserialized input DTO fields onto this command instance.
     * Called after the command is created by its domain factory and before
     * it is passed to the CommandHandler chain.
     */
    void applyInput(T input);
}
