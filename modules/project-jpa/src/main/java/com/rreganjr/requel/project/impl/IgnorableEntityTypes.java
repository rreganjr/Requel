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
package com.rreganjr.requel.project.impl;

import java.util.Map;

import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;

/**
 * Issue #320: the entity types an assistant finding, and so an ignored finding, can be on, keyed
 * by the discriminator the finding records ({@code getProjectOrDomainEntityInterface()
 * .getSimpleName()}).
 */
public final class IgnorableEntityTypes {

	public static final Map<String, Class<?>> BY_NAME = Map.of(
			"Goal", Goal.class,
			"Story", Story.class,
			"Actor", Actor.class,
			"UseCase", UseCase.class,
			"Scenario", Scenario.class,
			"Step", Step.class,
			"GlossaryTerm", GlossaryTerm.class,
			"ReportGenerator", ReportGenerator.class);

	private IgnorableEntityTypes() {
	}
}
