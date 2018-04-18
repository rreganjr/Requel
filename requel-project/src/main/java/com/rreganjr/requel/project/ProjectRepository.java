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

import com.rreganjr.EntityException;
import com.rreganjr.repository.Repository;
import com.rreganjr.NoSuchEntityException;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.project.impl.AddActorPosition;
import com.rreganjr.requel.project.impl.AddGlossaryTermPosition;
import com.rreganjr.requel.user.User;

/**
 * A repository for holding projects and project entities.
 * 
 * @author ron
 */
public interface ProjectRepository extends Repository {

	/**
	 * @param name -
	 *            the name of the project
	 * @return the project with the supplied name.
	 * @throws NoSuchProjectException -
	 *             if a project with the given name does not exist.
	 */
	public Project findProjectByName(String name) throws NoSuchProjectException;

	/**
	 * @param projectOrDomain -
	 *            the project or domain that contains the goal.
	 * @param name -
	 *            the name of the goal.
	 * @return the goal with the supplied name for the supplied project.
	 * @throws NoSuchEntityException -
	 *             if a goal does not exist for the project or domain and name.
	 */
	public Goal findGoalByProjectOrDomainAndName(ProjectOrDomain projectOrDomain, String name)
			throws NoSuchEntityException;

	/**
	 * @param projectOrDomain -
	 *            the project or domain that contains the usecase.
	 * @param name -
	 *            the name of the usecase.
	 * @return the usecase with the supplied name for the supplied project.
	 * @throws NoSuchEntityException -
	 *             if a usecase does not exist for the project or domain and
	 *             name.
	 */
	public UseCase findUseCaseByProjectOrDomainAndName(ProjectOrDomain projectOrDomain, String name)
			throws NoSuchEntityException;

	/**
	 * @param projectOrDomain -
	 *            the project or domain that contains the story.
	 * @param name -
	 *            the name of the story.
	 * @return the story with the supplied name for the supplied project.
	 * @throws NoSuchEntityException -
	 *             if a story does not exist for the project or domain and name.
	 */
	public Story findStoryByProjectOrDomainAndName(ProjectOrDomain projectOrDomain, String name)
			throws NoSuchEntityException;

	/**
	 * @param projectOrDomain -
	 *            the project or domain that contains the scenario.
	 * @param name -
	 *            the name of the scenario.
	 * @return
	 * @throws NoSuchEntityException
	 */
	public Scenario findScenarioByProjectOrDomainAndName(ProjectOrDomain projectOrDomain,
			String name) throws NoSuchEntityException;

	/**
	 * @param usecase
	 * @return THe scenarios used by the supplied usecase
	 */
	public Set<Scenario> findScenariosUsedByUseCase(UseCase usecase);

	/**
	 * Find a non-user stakeholder by name.
	 * 
	 * @param projectOrDomain -
	 *            the project or domain that contains the stakeholder.
	 * @param name -
	 *            the name of the stakeholder.
	 * @return
	 * @throws NoSuchEntityException
	 */
	public NonUserStakeholder findStakeholderByProjectOrDomainAndName(ProjectOrDomain projectOrDomain,
			String name) throws NoSuchEntityException;

	/**
	 * Find a user stakeholder by user.
	 * 
	 * @param projectOrDomain -
	 *            the project or domain that contains the stakeholder.
	 * @param user -
	 *            the user.
	 * @return
	 * @throws NoSuchEntityException
	 */
	public UserStakeholder findStakeholderByProjectOrDomainAndUser(ProjectOrDomain projectOrDomain,
			User user) throws NoSuchEntityException;

	/**
	 * @param entityType -
	 *            the type of project entity (a subclass of
	 *            ProjectOrDomainEntity) the permission is for
	 * @param permissionType -
	 *            the permission type (Edit, Grant, etc.)
	 * @return The StakeholderPermission for the supplied entity type and
	 *         permission type.
	 * @throws EntityException -
	 *             if a permission doesn't exist for the given project entity
	 *             type and permission type.
	 */
	public StakeholderPermission findStakeholderPermission(Class<?> entityType,
			StakeholderPermissionType permissionType) throws EntityException;

	/**
	 * @return All the permissions available to grant on project entities.
	 */
	public Set<StakeholderPermission> findAvailableStakeholderPermissions();

	/**
	 * TODO: is this needed because the project or domain already holds its
	 * terms. Get a glossary term for the given project or domain.
	 * 
	 * @param projectOrDomain -
	 *            the project or domain to search for the term.
	 * @param name -
	 *            the term name
	 * @return
	 */
	public GlossaryTerm findGlossaryTermForProjectOrDomain(ProjectOrDomain projectOrDomain,
			String name);

	/**
	 * @param projectOrDomainEntity -
	 *            a project or domain entity to find all the terms for.
	 * @return
	 */
	public Set<GlossaryTerm> findGlossaryTermsForProjectOrDomainEntity(
			ProjectOrDomainEntity projectOrDomainEntity);

	/**
	 * @param projectOrDomain -
	 *            the project or domain to add the term to.
	 * @param term -
	 *            the term to add.
	 * @return
	 */
	public AddGlossaryTermPosition findAddGlossaryTermPosition(ProjectOrDomain projectOrDomain,
			String term);

	/**
	 * @param projectOrDomain
	 * @param name
	 * @return
	 * @throws EntityException
	 *             if an actor with the name doesn't exist for the project.
	 */
	public Actor findActorByProjectOrDomainAndName(ProjectOrDomain projectOrDomain, String name)
			throws EntityException;

	/**
	 * @param projectOrDomain -
	 *            the project or domain to add the actor to.
	 * @param actorName -
	 *            the actor name.
	 * @return
	 */
	public AddActorPosition findAddActorPosition(ProjectOrDomain projectOrDomain, String actorName);

	/**
	 * @param projectOrDomain -
	 *            the project or domain that contains the report generator.
	 * @param name -
	 *            the name of the report generator.
	 * @return
	 * @throws EntityException
	 */
	public ReportGenerator findReportGeneratorByProjectOrDomainAndName(
			ProjectOrDomain projectOrDomain, String name) throws EntityException;
}
