/*
 * $Id: ProjectRepository.java,v 1.17 2009/02/16 10:10:09 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */

package edu.harvard.fas.rregan.requel.project;

import java.util.Set;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.Repository;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.project.exception.NoSuchProjectException;
import edu.harvard.fas.rregan.requel.project.impl.AddActorPosition;
import edu.harvard.fas.rregan.requel.project.impl.AddGlossaryTermPosition;
import edu.harvard.fas.rregan.requel.user.User;

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
	public Stakeholder findStakeholderByProjectOrDomainAndName(ProjectOrDomain projectOrDomain,
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
	public Stakeholder findStakeholderByProjectOrDomainAndUser(ProjectOrDomain projectOrDomain,
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