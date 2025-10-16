/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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

import java.util.Set;
import java.util.SortedSet;

import com.rreganjr.requel.CreatedEntity;
import com.rreganjr.requel.Describable;

/**
 * @author ron
 */
public interface ProjectOrDomain extends CreatedEntity, Describable, GoalContainer, StoryContainer,
		ActorContainer, ScenarioContainer {

	/**
	 * @return The name used to uniquely idenfity the domain or project.
	 */
	public String getName();

	/**
	 * Change the name of the domain or project.
	 * 
	 * @param name
	 */
	public void setName(String name);

	/**
	 * @return get the description of this project or domain.
	 */
	public String getText();

	/**
	 * set the description of the project or domain.
	 * 
	 * @param description
	 */
	public void setText(String description);

	/**
	 * @return a set of terms specific to this domain or project
	 */
	public SortedSet<GlossaryTerm> getGlossaryTerms();

	/**
	 * @return The set of all use cases defined for this domain or project.
	 */
	public Set<UseCase> getUseCases();

	/**
	 * @return The set of all stakeholders defined for this domain or project.
	 *         For a domain this should only be non-user stakeholders that will
	 *         be consistent for future projects.
	 */
	public Set<Stakeholder> getStakeholders();

	/**
	 * @return
	 */
	public Set<ProjectTeam> getTeams();

	/**
	 * @return The set of report generators for the project.
	 */
	public Set<ReportGenerator> getReportGenerators();

	/**
	 * @return all the project entities (goals, actors, stakeholders, etc.)
	 */
	public Set<ProjectOrDomainEntity> getProjectEntities();

}
