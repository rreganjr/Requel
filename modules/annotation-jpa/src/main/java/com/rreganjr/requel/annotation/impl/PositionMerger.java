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
package com.rreganjr.requel.annotation.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rreganjr.requel.annotation.Argument;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;

import jakarta.persistence.EntityManager;

/**
 * Merges positions that share text within one grouping object into a single surviving position
 * (issue #284).
 * <p>
 * Lives in this package deliberately: {@code ArgumentImpl.setPosition} and
 * {@code IssueImpl.setResolvedByPosition} are {@code protected}, and reparenting through them is
 * the whole job. The alternative — widening those setters, or doing the reparent in native SQL —
 * would make the relationship writable from anywhere, or invisible to the session.
 * <p>
 * <strong>Lowest id wins.</strong> The caller supplies the duplicates ordered by id ascending and
 * the first is the survivor, which is the same rule
 * {@code JpaAnnotationRepository.findPosition} uses to pick one, so a lookup and a merge can never
 * disagree about who won.
 * <p>
 * <strong>Order matters.</strong> {@code PositionImpl.arguments} is
 * {@code @OneToMany(cascade = ALL)}, so deleting a loser deletes its arguments — the debate
 * content. Every argument is therefore reparented and the loser's collection emptied <em>before</em>
 * the delete, and the whole thing is flushed in between.
 *
 * @author ron
 */
class PositionMerger {

	private static final Logger log = LoggerFactory.getLogger(PositionMerger.class);

	private final EntityManager entityManager;

	PositionMerger(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * @param duplicates two or more positions sharing text in one grouping object, ordered by id
	 *            ascending.
	 * @return the surviving position — the first element.
	 */
	Position merge(List<Position> duplicates) {
		PositionImpl winner = (PositionImpl) duplicates.get(0);
		List<Position> losers = new ArrayList<>(duplicates.subList(1, duplicates.size()));
		log.info("merging {} duplicate position(s) into position {} (\"{}\")", losers.size(),
				winner.getId(), winner.getText());

		for (Position each : losers) {
			PositionImpl loser = (PositionImpl) each;
			unionIssueLinks(winner, loser);
			reparentArguments(winner, loser);
			repointResolutions(winner, loser);
		}
		// Flush the reparenting before anything is removed: the arguments must already belong to
		// the winner when the loser goes, or cascade = ALL takes them with it.
		entityManager.flush();

		for (Position each : losers) {
			PositionImpl loser = (PositionImpl) each;
			// Same reasoning as AnnotationRepository.unlinkAllAnnotations (#247/#248): Hibernate
			// skips the join-table delete for a collection whose loaded snapshot was empty, so a
			// link committed between load and delete would outlive the row and fail the FK.
			entityManager.createNativeQuery(
					"DELETE FROM position_issue WHERE position_id = :positionId")
					.setParameter("positionId", loser.getId())
					.executeUpdate();
			entityManager.remove(loser);
		}
		entityManager.flush();
		return winner;
	}

	private void unionIssueLinks(PositionImpl winner, PositionImpl loser) {
		for (Issue issue : new ArrayList<>(loser.getIssues())) {
			winner.getIssues().add(issue);
			issue.getPositions().remove(loser);
			issue.getPositions().add(winner);
		}
		loser.getIssues().clear();
	}

	private void reparentArguments(PositionImpl winner, PositionImpl loser) {
		for (Argument argument : new ArrayList<>(loser.getArguments())) {
			((ArgumentImpl) argument).setPosition(winner);
			winner.getArguments().add(argument);
		}
		loser.getArguments().clear();
	}

	private void repointResolutions(PositionImpl winner, PositionImpl loser) {
		List<?> resolved = entityManager
				.createQuery("select issue from IssueImpl issue "
						+ "where issue.resolvedByPosition = :position")
				.setParameter("position", loser)
				.getResultList();
		for (Object each : resolved) {
			((IssueImpl) each).setResolvedByPosition(winner);
		}
	}
}
