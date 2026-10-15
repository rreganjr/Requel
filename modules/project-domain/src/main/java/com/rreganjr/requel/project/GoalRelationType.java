/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025, 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import com.rreganjr.requel.DescribedValue;

/**
 * The type of relationship a {@link GoalRelation} declares from its from goal to its to goal.
 *
 * <p>
 * This is model semantics, not content: a small closed vocabulary every reader of a model must
 * interpret the same way, so it is not project- or user-editable (projects that need their own
 * relationship semantics have tags). The enum is the only place a value is declared; the UI
 * picker, the gateway input schema and validation messages all read the metadata here. Persisted
 * by name ({@code goal_relations.relation_type}, a {@code VARCHAR} since V23), so adding a value
 * is not a column change. Issue #257.
 *
 * <p>
 * A relation is stored in the direction it was entered. For a directed type the label reads from
 * the from goal ("A Supports B") and the inverse label from the to goal ("B Supported by A"). A
 * symmetric type holds in both directions and uses the same word either way.
 *
 * @author ron
 */
public enum GoalRelationType implements DescribedValue {

	/** The from goal has a positive influence on the success of the to goal. */
	Supports("Supports", "Supported by", false,
			"The from goal has a positive influence on the success of the to goal."),

	/** The from goal and the to goal cannot both be satisfied. */
	Conflicts("Conflicts", "Conflicts", true, "The two goals cannot both be satisfied."),

	/** The from goal is a more concrete statement of the to goal. */
	Refines("Refines", "Refined by", false,
			"The from goal is a more concrete statement of the to goal."),

	/** The from goal and the to goal state the same requirement. */
	Duplicates("Duplicates", "Duplicates", true, "The two goals state the same requirement."),

	/** The from goal cannot be achieved until the to goal is. */
	DependsOn("Depends on", "Required by", false,
			"The from goal cannot be achieved until the to goal is."),

	/** The from goal makes the to goal harder without making it unsatisfiable. */
	Obstructs("Obstructs", "Obstructed by", false,
			"The from goal makes the to goal harder without making it unsatisfiable."),

	/** The from goal is the measurable proxy for the outcome stated by the to goal. */
	Measures("Measures", "Measured by", false,
			"The from goal is the measurable proxy for the outcome stated by the to goal.");

	private final String label;
	private final String inverseLabel;
	private final boolean symmetric;
	private final String description;

	GoalRelationType(String label, String inverseLabel, boolean symmetric, String description) {
		this.label = label;
		this.inverseLabel = inverseLabel;
		this.symmetric = symmetric;
		this.description = description;
	}

	/**
	 * @return the display name, read from the from goal's side ("Depends on").
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the display name read from the to goal's side ("Required by"); the same as
	 *         {@link #getLabel()} for a symmetric type.
	 */
	public String getInverseLabel() {
		return inverseLabel;
	}

	/**
	 * @return true when the relation holds in both directions (A Conflicts B is B Conflicts A).
	 */
	public boolean isSymmetric() {
		return symmetric;
	}

	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * Case-insensitive, surrounding whitespace ignored, matched against the value name
	 * ({@code "DependsOn"}), not the label.
	 *
	 * @param value
	 *            a relation type name such as {@code "supports"}
	 * @return the type, or empty for a null, blank or unrecognised value
	 */
	public static Optional<GoalRelationType> parse(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		String name = value.trim().toLowerCase(Locale.ROOT);
		for (GoalRelationType type : values()) {
			if (type.name().toLowerCase(Locale.ROOT).equals(name)) {
				return Optional.of(type);
			}
		}
		return Optional.empty();
	}

	/**
	 * @return every value name in declaration order, comma separated, for messages that tell a
	 *         caller what it may send.
	 */
	public static String permittedValues() {
		return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
	}
}
