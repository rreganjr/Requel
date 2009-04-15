/*
 * $Id: ProjectOrDomain.java,v 1.12 2008/12/31 11:49:33 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project;

import java.util.Set;
import java.util.SortedSet;

import edu.harvard.fas.rregan.requel.CreatedEntity;
import edu.harvard.fas.rregan.requel.Describable;

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
