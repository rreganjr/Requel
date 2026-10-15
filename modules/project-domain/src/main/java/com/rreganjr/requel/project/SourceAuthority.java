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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Issue #273: precedence between a project's sources, worked out from its
 * {@link SourceAuthorityEdge}s. Pure: it takes the edges as source ids and never touches the
 * database, so the store, the reads and the tests share one rule.
 * <p>
 * "A defers to B" means B wins where the two disagree. Precedence is transitive: if A defers to B
 * and B to C, C wins over A. Sources with no path between them are unordered.
 */
public final class SourceAuthority {

	/** Which of two sources wins. */
	public enum Winner {
		/** The first source named. */
		A,
		/** The second source named. */
		B,
		/** Neither: there is no chain of deference between them (or they are the same source). */
		NONE
	}

	/**
	 * The outcome of comparing two sources.
	 *
	 * @param winner which one wins
	 * @param chain  the source ids from the loser up to the winner, both included; empty for
	 *               {@link Winner#NONE}
	 */
	public record Resolution(Winner winner, List<Long> chain) {
	}

	/** One edge as ids: {@code subordinate} defers to {@code superior}. */
	public record Edge(Long subordinate, Long superior) {
		public Edge {
			Objects.requireNonNull(subordinate, "subordinate");
			Objects.requireNonNull(superior, "superior");
		}
	}

	private SourceAuthority() {
	}

	/** The edges as ids. */
	public static List<Edge> edgesOf(Collection<? extends SourceAuthorityEdge> edges) {
		List<Edge> ids = new ArrayList<>();
		for (SourceAuthorityEdge edge : edges) {
			ids.add(new Edge(edge.getSubordinate().getId(), edge.getSuperior().getId()));
		}
		return ids;
	}

	/**
	 * Which of {@code a} and {@code b} wins.
	 */
	public static Resolution resolve(Collection<Edge> edges, Long a, Long b) {
		if (a == null || b == null || a.equals(b)) {
			return new Resolution(Winner.NONE, List.of());
		}
		Map<Long, List<Long>> up = superiors(edges);
		List<Long> aUp = path(up, a, b);
		if (!aUp.isEmpty()) {
			return new Resolution(Winner.B, aUp);
		}
		List<Long> bUp = path(up, b, a);
		if (!bUp.isEmpty()) {
			return new Resolution(Winner.A, bUp);
		}
		return new Resolution(Winner.NONE, List.of());
	}

	/**
	 * True when adding "{@code subordinate} defers to {@code superior}" would close a cycle — that
	 * is, the superior already defers, directly or not, to the subordinate — or is a self-edge.
	 */
	public static boolean wouldCycle(Collection<Edge> edges, Long subordinate, Long superior) {
		if (subordinate == null || superior == null) {
			return false;
		}
		return subordinate.equals(superior) || !path(superiors(edges), superior, subordinate).isEmpty();
	}

	/** The sources {@code source} defers to directly, in edge order. */
	public static List<Long> superiorsOf(Collection<Edge> edges, Long source) {
		return List.copyOf(superiors(edges).getOrDefault(source, List.of()));
	}

	/** The sources that defer directly to {@code source}, in edge order. */
	public static List<Long> subordinatesOf(Collection<Edge> edges, Long source) {
		List<Long> result = new ArrayList<>();
		for (Edge edge : edges) {
			if (edge.superior().equals(source)) {
				result.add(edge.subordinate());
			}
		}
		return result;
	}

	private static Map<Long, List<Long>> superiors(Collection<Edge> edges) {
		Map<Long, List<Long>> up = new HashMap<>();
		for (Edge edge : edges) {
			List<Long> list = up.computeIfAbsent(edge.subordinate(), k -> new ArrayList<>());
			if (!list.contains(edge.superior())) {
				list.add(edge.superior());
			}
		}
		return up;
	}

	/**
	 * The shortest chain from {@code from} up to {@code to} along defers-to edges, both ends
	 * included, or empty when there is none. Breadth-first with a visited set, so a cycle in stored
	 * data (which the store refuses to write) can never loop.
	 */
	private static List<Long> path(Map<Long, List<Long>> up, Long from, Long to) {
		Map<Long, Long> cameFrom = new HashMap<>();
		Set<Long> visited = new HashSet<>();
		Deque<Long> queue = new ArrayDeque<>();
		queue.add(from);
		visited.add(from);
		while (!queue.isEmpty()) {
			Long current = queue.poll();
			for (Long next : up.getOrDefault(current, List.of())) {
				if (!visited.add(next)) {
					continue;
				}
				cameFrom.put(next, current);
				if (next.equals(to)) {
					LinkedHashSet<Long> reversed = new LinkedHashSet<>();
					for (Long step = to; step != null; step = cameFrom.get(step)) {
						reversed.add(step);
					}
					List<Long> chain = new ArrayList<>(reversed);
					java.util.Collections.reverse(chain);
					return chain;
				}
				queue.add(next);
			}
		}
		return List.of();
	}
}
