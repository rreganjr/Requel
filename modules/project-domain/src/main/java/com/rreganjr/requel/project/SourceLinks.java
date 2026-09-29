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
package com.rreganjr.requel.project;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Issue #272: the derived state of an {@link EntitySourceLink}, shared by the ingest command and
 * the reads so both answer "was this entity edited in Requel since it was ingested?" the same way.
 */
public final class SourceLinks {

	private SourceLinks() {
	}

	/**
	 * The fingerprint an ingest records for an entity, and compares with to tell a Requel edit.
	 * It is {@link TargetFingerprint} (name and text) plus whatever else the wrapped edit command
	 * would overwrite on an update: a scenario's steps (in order), a use case's primary actor, and
	 * a story's primary actor and type. So a step edited in Requel, which leaves the scenario's
	 * own name and text alone, still makes a changed re-ingest a conflict rather than an
	 * overwrite. For an entity with none of these it is exactly {@link TargetFingerprint}, which is
	 * what #270 records and V26 infers against.
	 */
	public static String fingerprint(ProjectOrDomainEntity entity) {
		String base = TargetFingerprint.of(entity);
		StringBuilder extra = new StringBuilder();
		if (entity instanceof Scenario scenario && scenario.getSteps() != null) {
			for (Step step : scenario.getSteps()) {
				extra.append("\u0001step\u0000")
						.append(TargetFingerprint.of(step.getName(), step.getText()));
			}
		}
		if (entity instanceof UseCase useCase) {
			extra.append("\u0001actor\u0000").append(name(useCase.getPrimaryActor()));
		}
		if (entity instanceof Story story) {
			extra.append("\u0001actor\u0000").append(name(story.getPrimaryActor()))
					.append("\u0001type\u0000")
					.append(story.getStoryType() == null ? "" : story.getStoryType().name());
		}
		if (extra.length() == 0) {
			return base;
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(
					((base == null ? "" : base) + extra).getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}

	private static String name(Actor actor) {
		return actor == null || actor.getName() == null ? "" : actor.getName();
	}

	/**
	 * True when {@code entity} changed in Requel since the link's last ingest: its
	 * {@link #fingerprint} differs from the recorded one.
	 * <p>
	 * A link converted from a #71 provenance note has no recorded fingerprint (P6). #71 wrote the
	 * criterion text as the goal's text, so the entity is unedited if its text still hashes, with
	 * {@link CriterionHash}, to the fragment hash the note recorded; anything else, or no text to
	 * compare, reads as edited — the direction that raises a conflict rather than overwriting.
	 */
	public static boolean isEditedSinceIngest(EntitySourceLink link, ProjectOrDomainEntity entity) {
		if (link == null || entity == null) {
			return false;
		}
		if (link.getEntityFingerprint() != null) {
			return !link.getEntityFingerprint().equals(fingerprint(entity));
		}
		if (link.getFragmentHash() == null || !(entity instanceof TextEntity textEntity)
				|| textEntity.getText() == null) {
			return true;
		}
		return !link.getFragmentHash().equals(CriterionHash.of(textEntity.getText()));
	}
}
