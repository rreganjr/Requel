/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.rreganjr.requel.annotation.Annotation;

/**
 * Issue #75: what a stakeholder permission brings with it, in one place for the server and the
 * stakeholder page.
 * <ul>
 * <li><b>Implied</b> permissions are granted with another, because its operation changes an
 * entity that survives: editing a use case creates and edits its primary scenario
 * (Scenario[Edit]) and can create its primary actor (Actor[Edit]). The grid shows them checked
 * and disabled, with the reason.</li>
 * <li><b>Owned deletes</b> are not granted. A permission's operation deletes what it leaves
 * behind or alone owns - the notes only on a deleted goal, a deleted use case's unshared
 * scenarios - as a step authorized by that permission
 * ({@code com.rreganjr.platform.command.CascadeAuthorizable}). The grid flags the delete it
 * covers that way with an asterisk and a note, so nobody is surprised.</li>
 * </ul>
 * Removing an entity from the containers and relations it is in is part of deleting it and is
 * never listed.
 */
public final class StakeholderPermissionRules {

	/** {@code implied} is granted with {@code granted}. */
	public record Implied(String granted, String implied, String reason) {
	}

	/** Holding {@code granted} deletes {@code flagged} entities it owns. */
	public record OwnedDelete(String granted, String flagged, String note) {
	}

	/** Content types whose delete removes the notes and issues only on them. */
	private static final List<Class<?>> ANNOTATED = List.of(Goal.class, Actor.class, Story.class,
			UseCase.class, Scenario.class, GlossaryTerm.class, Stakeholder.class,
			ReportGenerator.class);

	public static final List<Implied> IMPLIED = List.of(
			new Implied(key(UseCase.class, StakeholderPermissionType.Edit),
					key(Scenario.class, StakeholderPermissionType.Edit),
					"Editing a use case creates and edits its primary scenario."),
			new Implied(key(UseCase.class, StakeholderPermissionType.Edit),
					key(Actor.class, StakeholderPermissionType.Edit),
					"Editing a use case creates its primary actor when it names a new one."));

	public static final List<OwnedDelete> OWNED_DELETES = ownedDeletes();

	private StakeholderPermissionRules() {
	}

	/** The permission key, as {@code StakeholderPermission.getPermissionKey()} writes it. */
	public static String key(Class<?> entityType, StakeholderPermissionType type) {
		return entityType.getName() + "[" + type + "]";
	}

	/** {@code com.rreganjr.requel.project.Goal[Edit]} as {@code Goal[Edit]}, for messages. */
	public static String label(String key) {
		int dot = key.lastIndexOf('.', key.indexOf('['));
		return dot < 0 ? key : key.substring(dot + 1);
	}

	/** {@code keys} with every implied permission added, transitively. */
	public static Set<String> closure(Collection<String> keys) {
		Set<String> closed = new LinkedHashSet<>(keys);
		boolean added = true;
		while (added) {
			added = false;
			for (Implied rule : IMPLIED) {
				if (closed.contains(rule.granted()) && closed.add(rule.implied())) {
					added = true;
				}
			}
		}
		return closed;
	}

	/**
	 * The permission that lets a stakeholder grant or remove {@code key}: Grant on its entity type,
	 * or Project[Grant] for a type with no Grant permission of its own (AssistantDefinition).
	 *
	 * @param catalog every permission key there is
	 */
	public static String grantKeyFor(String key, Collection<String> catalog) {
		String type = key.substring(0, key.indexOf('['));
		String grant = type + "[" + StakeholderPermissionType.Grant + "]";
		return catalog.contains(grant) ? grant : key(Project.class, StakeholderPermissionType.Grant);
	}

	private static List<OwnedDelete> ownedDeletes() {
		List<OwnedDelete> rules = new ArrayList<>();
		String annotationDelete = key(Annotation.class, StakeholderPermissionType.Delete);
		rules.add(new OwnedDelete(key(UseCase.class, StakeholderPermissionType.Delete),
				key(Scenario.class, StakeholderPermissionType.Delete),
				"Deleting a use case deletes its scenarios and steps that no other use case uses."));
		rules.add(new OwnedDelete(key(Scenario.class, StakeholderPermissionType.Edit),
				key(Scenario.class, StakeholderPermissionType.Delete),
				"Removing a step from a scenario deletes the step when no other scenario uses it."));
		for (Class<?> type : ANNOTATED) {
			rules.add(new OwnedDelete(key(type, StakeholderPermissionType.Delete), annotationDelete,
					"Deleting " + article(type) + " deletes the notes and issues that are only on it."));
		}
		rules.add(new OwnedDelete(key(Goal.class, StakeholderPermissionType.Edit), annotationDelete,
				"Removing a goal relation deletes the notes and issues that are only on it."));
		rules.add(new OwnedDelete(key(Project.class, StakeholderPermissionType.Edit),
				annotationDelete,
				"Restoring an ignored finding removes the issue it had resolved."));
		String projectDelete = key(Project.class, StakeholderPermissionType.Delete);
		List<Class<?>> everything = new ArrayList<>(ANNOTATED);
		everything.add(Annotation.class);
		for (Class<?> type : everything) {
			rules.add(new OwnedDelete(projectDelete, key(type, StakeholderPermissionType.Delete),
					"Deleting the project deletes everything in it."));
		}
		return List.copyOf(rules);
	}

	private static String article(Class<?> type) {
		String name = switch (type.getSimpleName()) {
			case "UseCase" -> "use case";
			case "GlossaryTerm" -> "glossary term";
			case "ReportGenerator" -> "report generator";
			default -> type.getSimpleName().toLowerCase(java.util.Locale.ROOT);
		};
		return ("aeiou".indexOf(name.charAt(0)) >= 0 ? "an " : "a ") + name;
	}
}
