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
package com.rreganjr.requel.annotation.impl;

import org.hibernate.proxy.HibernateProxy;

import com.rreganjr.repository.jpa.ProxyTypes;
import com.rreganjr.requel.annotation.Position;

/**
 * Resolves what a {@link Position} actually is, without asking the object for its class name.
 * <p>
 * A position reaching an API mapper or a command factory may be a CGLIB EntityProxy (everything
 * returned by a repository method or a {@code Command} getter), a Hibernate proxy (a lazy
 * association), or a plain instance. {@code getClass().getSimpleName()} answers the first two with a
 * generated name carrying a build-specific hash — the defect in issue #253 — so resolution goes
 * through the persisted discriminator instead, in three tiers:
 * <ol>
 * <li>a Hibernate proxy's entity name, which is available without initializing it;</li>
 * <li>the {@code position_type} discriminator on {@link PositionImpl}, which survives any
 * proxying because reading it goes through the proxy to the real entity;</li>
 * <li>the user class behind the proxy, for a transient position whose discriminator is not set
 * yet.</li>
 * </ol>
 * Both the API DTO mapper and
 * {@code AnnotationCommandFactoryImpl.newResolveIssueCommand} resolve through here, so the type a
 * client is told about and the resolver the server picks can never disagree.
 *
 * @author ron
 */
public final class PositionTypes {

	private static final String IMPL_SUFFIX = "Impl";

	private PositionTypes() {
		// static helpers only
	}

	/**
	 * The API-facing type name: the discriminator with its package stripped and a trailing
	 * {@code Impl} removed, e.g. {@code "AddActorPosition"} or, for a plain position,
	 * {@code "Position"}.
	 * <p>
	 * Stable across runs and across builds, and never contains a generated proxy name.
	 *
	 * @param position
	 *            possibly proxied, possibly null
	 * @return the pretty type name, or null when the position is null
	 */
	public static String typeNameOf(Position position) {
		String entityName = rawTypeNameOf(position);
		return entityName == null ? null : prettyName(entityName);
	}

	/**
	 * The persistent entity name (a fully qualified class name) behind a position. This is the key
	 * the resolver registries are built on, so it is deliberately <em>not</em> prettified.
	 *
	 * @param position
	 *            possibly proxied, possibly null
	 * @return the entity name, or null when the position is null
	 */
	public static String rawTypeNameOf(Position position) {
		if (position == null) {
			return null;
		}
		// 1. a Hibernate proxy knows its entity name without being initialized
		if (position instanceof HibernateProxy) {
			String entityName = ((HibernateProxy) position).getHibernateLazyInitializer()
					.getEntityName();
			if (entityName != null) {
				return entityName;
			}
		}
		// 2. the persisted discriminator, read through whatever proxy is in the way
		if (position instanceof PositionImpl) {
			String discriminator = ((PositionImpl) position).getType();
			if (discriminator != null) {
				return discriminator;
			}
		}
		// 3. a transient position that has not been given a discriminator yet. userClassOf only
		// answers null for a null argument, and position is non-null by here.
		return ProxyTypes.userClassOf(position).getName();
	}

	/**
	 * Strip the package and a trailing {@code Impl}. Also stops at the first {@code $} as a
	 * belt-and-braces guard: no caller should ever reach here with a generated class name, but if
	 * one does, the API must not be the place it surfaces.
	 */
	private static String prettyName(String entityName) {
		String simpleName = entityName.substring(entityName.lastIndexOf('.') + 1);
		int generatedSuffix = simpleName.indexOf('$');
		if (generatedSuffix > 0) {
			simpleName = simpleName.substring(0, generatedSuffix);
		}
		if ((simpleName.length() > IMPL_SUFFIX.length()) && simpleName.endsWith(IMPL_SUFFIX)) {
			simpleName = simpleName.substring(0, simpleName.length() - IMPL_SUFFIX.length());
		}
		return simpleName;
	}
}
