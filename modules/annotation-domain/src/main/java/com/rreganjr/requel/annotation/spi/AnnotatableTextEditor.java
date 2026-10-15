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
package com.rreganjr.requel.annotation.spi;

import com.rreganjr.command.Command;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Annotatable;

/**
 * Builds the {@code Edit*Command} that changes one named text property of an annotatable
 * entity (issue #305).
 * <p>
 * Resolving a spelling issue has to change the annotated entity's text. It used to do that
 * with reflective setters on the managed instance, which bypassed authorization, optimistic
 * locking and audit alike. Instead the resolver asks this for a command and runs it through
 * the command handler, so the edit is authorized as that entity's own edit.
 * <p>
 * The property name is whatever {@code LexicalIssue.getAnnotatableEntityPropertyName()} holds
 * for the issue being resolved &mdash; in practice the name or text property constants the
 * analysing assistant used.
 *
 * @author ron
 */
public interface AnnotatableTextEditor {

	/**
	 * Read the current value of a text property, so a caller can compute a correction from it.
	 * <p>
	 * Paired with {@link #newEditCommand} on purpose: whatever knows how to write a property
	 * through the right command also knows how to read it, and splitting the two is how the
	 * reflective accessors this replaced drifted out of step with the commands.
	 *
	 * @param annotatable the entity to read
	 * @param propertyName the text property to read
	 * @return the current text, which may be null
	 * @throws IllegalArgumentException if this editor cannot read that property on that entity
	 */
	String currentValue(Annotatable annotatable, String propertyName);

	/**
	 * Build a configured but <em>unexecuted</em> command that sets {@code propertyName} to
	 * {@code newValue} on {@code annotatable}.
	 * <p>
	 * Implementations must leave the entity's other text properties as they are. They do that
	 * by reading the current values off the entity and supplying every property to the
	 * command, not by leaving the others unset: several {@code Edit*Command} implementations
	 * overwrite an unsupplied property with null rather than skipping it (issue #316).
	 *
	 * @param annotatable the entity whose text is being corrected
	 * @param propertyName the text property to change
	 * @param newValue the corrected text for that property
	 * @param editedBy the user the edit is attributed to
	 * @return a command ready to hand to the command handler, never null
	 * @throws IllegalArgumentException if this editor cannot set that property on that entity
	 */
	Command newEditCommand(Annotatable annotatable, String propertyName, String newValue,
			User editedBy);
}
