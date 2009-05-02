/*
 * $Id: JpaProjectRepository.java,v 1.32 2009/02/16 10:10:08 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.requel.project.impl.repository.jpa;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.NoResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.Query;

import org.hibernate.PropertyValueException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.validator.InvalidStateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.repository.jpa.AbstractJpaRepository;
import edu.harvard.fas.rregan.repository.jpa.ConstraintViolationExceptionAdapter;
import edu.harvard.fas.rregan.repository.jpa.ExceptionMapper;
import edu.harvard.fas.rregan.repository.jpa.GenericPropertyValueExceptionAdapter;
import edu.harvard.fas.rregan.repository.jpa.InvalidStateExceptionAdapter;
import edu.harvard.fas.rregan.repository.jpa.OptimisticLockExceptionAdapter;
import edu.harvard.fas.rregan.requel.NoSuchEntityException;
import edu.harvard.fas.rregan.requel.annotation.NoSuchPositionException;
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.ReportGenerator;
import edu.harvard.fas.rregan.requel.project.Scenario;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.project.StakeholderPermission;
import edu.harvard.fas.rregan.requel.project.StakeholderPermissionType;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.UseCase;
import edu.harvard.fas.rregan.requel.project.command.EditProjectCommand;
import edu.harvard.fas.rregan.requel.project.exception.NoSuchActorException;
import edu.harvard.fas.rregan.requel.project.exception.NoSuchGlossaryTermException;
import edu.harvard.fas.rregan.requel.project.exception.NoSuchProjectException;
import edu.harvard.fas.rregan.requel.project.impl.AddActorPosition;
import edu.harvard.fas.rregan.requel.project.impl.AddGlossaryTermPosition;
import edu.harvard.fas.rregan.requel.project.impl.GlossaryTermImpl;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * EJB3/JPA based repository
 * 
 * @author ron
 */
@Repository("projectRepository")
@Scope("singleton")
@Transactional(propagation = Propagation.REQUIRED, noRollbackFor = { NoSuchPositionException.class,
		NoSuchProjectException.class, EntityException.class })
public class JpaProjectRepository extends AbstractJpaRepository implements ProjectRepository {

	/**
	 * @param exceptionMapper
	 */
	@Autowired
	public JpaProjectRepository(ExceptionMapper exceptionMapper) {
		super(exceptionMapper);
		addExceptionAdapter(PropertyValueException.class,
				new GenericPropertyValueExceptionAdapter(), Project.class,
				ProjectOrDomainEntity.class, AddGlossaryTermPosition.class, AddActorPosition.class);

		addExceptionAdapter(InvalidStateException.class, new InvalidStateExceptionAdapter(),
				Project.class, ProjectOrDomainEntity.class, AddGlossaryTermPosition.class,
				AddActorPosition.class);

		addExceptionAdapter(ConstraintViolationException.class,
				new ConstraintViolationExceptionAdapter(EditProjectCommand.FIELD_NAME),
				Project.class, ProjectOrDomainEntity.class, AddGlossaryTermPosition.class,
				AddActorPosition.class);

		addExceptionAdapter(OptimisticLockException.class, new OptimisticLockExceptionAdapter(),
				Project.class, ProjectOrDomainEntity.class, AddGlossaryTermPosition.class,
				AddActorPosition.class);

		addExceptionAdapter(StaleObjectStateException.class, new OptimisticLockExceptionAdapter(),
				Project.class, ProjectOrDomainEntity.class, AddGlossaryTermPosition.class,
				AddActorPosition.class);

		addExceptionAdapter(LockAcquisitionException.class, new OptimisticLockExceptionAdapter(),
				Project.class, ProjectOrDomainEntity.class, AddGlossaryTermPosition.class,
				AddActorPosition.class);

		addExceptionAdapter(CannotAcquireLockException.class, new OptimisticLockExceptionAdapter(),
				Project.class, ProjectOrDomainEntity.class, AddGlossaryTermPosition.class,
				AddActorPosition.class);

		addExceptionAdapter(HibernateOptimisticLockingFailureException.class,
				new OptimisticLockExceptionAdapter(), Project.class, ProjectOrDomainEntity.class,
				AddGlossaryTermPosition.class, AddActorPosition.class);
	}

	public Project findProjectByName(String name) throws NoSuchProjectException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(project) from ProjectImpl as project where project.name like :name");
			query.setParameter("name", name.trim());
			return (Project) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchProjectException.forName(name);
		} catch (Exception e) {
			throw convertException(e, Project.class, null, EntityExceptionActionType.Reading);
		}
	}

	@Override
	public Goal findGoalByProjectOrDomainAndName(ProjectOrDomain pod, String name)
			throws EntityException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(goal) from GoalImpl as goal where goal.projectOrDomain = :projectOrDomain and goal.name like :name");
			query.setParameter("projectOrDomain", pod);
			query.setParameter("name", name.trim());
			return (Goal) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchEntityException.byQuery(Goal.class, new String[] { "project", "name" },
					new Object[] { pod, name });
		} catch (Exception e) {
			throw convertException(e, Goal.class, null, EntityExceptionActionType.Reading);
		}
	}

	@Override
	public Scenario findScenarioByProjectOrDomainAndName(ProjectOrDomain pod, String name)
			throws NoSuchEntityException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(scenario) from ScenarioImpl as scenario where scenario.projectOrDomain = :projectOrDomain and scenario.name like :name");
			query.setParameter("projectOrDomain", pod);
			query.setParameter("name", name.trim());
			return (Scenario) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchEntityException.byQuery(Scenario.class, new String[] { "project", "name" },
					new Object[] { pod, name });
		} catch (Exception e) {
			throw convertException(e, Scenario.class, null, EntityExceptionActionType.Reading);
		}
	}

	@Override
	public Set<Scenario> findScenariosUsedByUseCase(UseCase usecase) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(scenario) from ScenarioImpl as scenario inner join scenario.usingUseCases as useCases where :usecase in useCases");
			query.setParameter("usecase", usecase);
			return new HashSet<Scenario>(query.getResultList());
		} catch (NoResultException e) {
			throw NoSuchEntityException.byQuery(Scenario.class, "usecase", usecase);
		} catch (Exception e) {
			throw convertException(e, Scenario.class, null, EntityExceptionActionType.Reading);
		}
	}

	@Override
	public Stakeholder findStakeholderByProjectOrDomainAndName(ProjectOrDomain projectOrDomain,
			String name) throws NoSuchEntityException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(stakeholder) from StakeholderImpl as stakeholder where stakeholder.projectOrDomain = :projectOrDomain and stakeholder.name like :name and stakeholder.user is null");
			query.setParameter("projectOrDomain", projectOrDomain);
			query.setParameter("name", name.trim());
			return (Stakeholder) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchEntityException.byQuery(Scenario.class, new String[] { "project", "name" },
					new Object[] { projectOrDomain, name });
		} catch (Exception e) {
			throw convertException(e, Scenario.class, null, EntityExceptionActionType.Reading);
		}
	}

	@Override
	public Stakeholder findStakeholderByProjectOrDomainAndUser(ProjectOrDomain projectOrDomain,
			User user) throws NoSuchEntityException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(stakeholder) from StakeholderImpl as stakeholder where stakeholder.projectOrDomain = :projectOrDomain and stakeholder.user = :user");
			query.setParameter("projectOrDomain", projectOrDomain);
			query.setParameter("user", user);
			return (Stakeholder) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchEntityException.byQuery(Scenario.class, new String[] { "project", "user" },
					new Object[] { projectOrDomain, user });
		} catch (Exception e) {
			throw convertException(e, Scenario.class, null, EntityExceptionActionType.Reading);
		}
	}

	public StakeholderPermission findStakeholderPermission(Class<?> entityType,
			StakeholderPermissionType permissionType) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(permission) from StakeholderPermissionImpl as permission where permission.entityType = :entityType and permission.permissionType = :permissionType");
			query.setParameter("entityType", entityType);
			query.setParameter("permissionType", permissionType);
			return (StakeholderPermission) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchEntityException.byQuery(StakeholderPermission.class, new String[] {
					"entityType", "permissionType" }, new Object[] { entityType, permissionType });
		} catch (Exception e) {
			throw convertException(e, StakeholderPermission.class, null,
					EntityExceptionActionType.Reading);
		}
	}

	public Set<StakeholderPermission> findAvailableStakeholderPermissions() {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(permission) from StakeholderPermissionImpl as permission");
			return new TreeSet<StakeholderPermission>(query.getResultList());
		} catch (Exception e) {
			throw convertException(e, StakeholderPermission.class, null,
					EntityExceptionActionType.Reading);
		}
	}

	@Override
	public GlossaryTerm findGlossaryTermForProjectOrDomain(ProjectOrDomain projectOrDomain,
			String name) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(term) from GlossaryTermImpl as term "
							+ "where term.name = :name and "
							+ "term.projectOrDomain = :projectOrDomain");
			query.setParameter("name", name.trim());
			query.setParameter("projectOrDomain", projectOrDomain);
			return (GlossaryTerm) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchGlossaryTermException.forProjectOrDomainWithName(projectOrDomain, name);
		} catch (Exception e) {
			throw convertException(e, GlossaryTerm.class, null, EntityExceptionActionType.Reading);
		}
	}

	@Override
	public Set<GlossaryTerm> findGlossaryTermsForProjectOrDomainEntity(
			ProjectOrDomainEntity projectOrDomainEntity) {
		try {
			String entityType = projectOrDomainEntity.getProjectOrDomainEntityInterface().getName();
			Object entityId = getId(projectOrDomainEntity);

			// NOTE: there isn't a way to create an HQL query that crosses an
			// ANY relationship
			// boundary, so a native query is the only way
			Query query = getEntityManager()
					.createNativeQuery(
							"select term.* from terms term left outer join terms_referers referers on term.id=referers.term_id where referers.referer_type = :entityType and referers.referer_id = :entityId",
							GlossaryTermImpl.class);
			query.setParameter("entityType", entityType);
			query.setParameter("entityId", entityId);
			return new HashSet<GlossaryTerm>(query.getResultList());
		} catch (Exception e) {
			throw convertException(e, GlossaryTerm.class, null, EntityExceptionActionType.Reading);
		}
	}

	public AddGlossaryTermPosition findAddGlossaryTermPosition(ProjectOrDomain projectOrDomain,
			String term) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(position) from AddGlossaryTermPosition as position "
							+ "left join position.issues as issue "
							+ "where issue.groupingObject = :projectOrDomain and "
							+ "issue.word like :term");
			query.setParameter("term", term.trim());
			query.setParameter("projectOrDomain", projectOrDomain);
			return (AddGlossaryTermPosition) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchPositionException.forAddingWordToGlossary(projectOrDomain, term);
		} catch (Exception e) {
			throw convertException(e, AddGlossaryTermPosition.class, null,
					EntityExceptionActionType.Reading);
		}
	}

	@Override
	public AddActorPosition findAddActorPosition(ProjectOrDomain projectOrDomain, String actorName) {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager().createQuery(
					"select object(position) from AddActorPosition as position "
							+ "left join position.issues as issue "
							+ "where issue.groupingObject = :projectOrDomain and "
							+ "issue.word like :actorName");
			query.setParameter("actorName", actorName.trim());
			query.setParameter("projectOrDomain", projectOrDomain);
			return (AddActorPosition) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchPositionException.forAddingWordToGlossary(projectOrDomain, actorName);
		} catch (Exception e) {
			throw convertException(e, AddActorPosition.class, null,
					EntityExceptionActionType.Reading);
		}
	}

	@Override
	public Actor findActorByProjectOrDomainAndName(ProjectOrDomain projectOrDomain, String name)
			throws EntityException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(actor) from ActorImpl as actor where actor.projectOrDomain = :projectOrDomain and actor.name like :name");
			query.setParameter("projectOrDomain", projectOrDomain);
			query.setParameter("name", name.trim());
			return (Actor) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchActorException.forProjectOrDomainWithName(projectOrDomain, name);
		} catch (Exception e) {
			throw convertException(e, Actor.class, null, EntityExceptionActionType.Reading);
		}
	}

	@Override
	public Story findStoryByProjectOrDomainAndName(ProjectOrDomain projectOrDomain, String name)
			throws EntityException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(story) from StoryImpl as story where story.projectOrDomain = :projectOrDomain and story.name like :name");
			query.setParameter("projectOrDomain", projectOrDomain);
			query.setParameter("name", name.trim());
			return (Story) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchActorException.forProjectOrDomainWithName(projectOrDomain, name);
		} catch (Exception e) {
			throw convertException(e, Story.class, null, EntityExceptionActionType.Reading);
		}
	}

	@Override
	public UseCase findUseCaseByProjectOrDomainAndName(ProjectOrDomain projectOrDomain, String name)
			throws EntityException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(usecase) from UseCaseImpl as usecase where usecase.projectOrDomain = :projectOrDomain and usecase.name like :name");
			query.setParameter("projectOrDomain", projectOrDomain);
			query.setParameter("name", name.trim());
			return (UseCase) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchActorException.forProjectOrDomainWithName(projectOrDomain, name);
		} catch (Exception e) {
			throw convertException(e, UseCase.class, null, EntityExceptionActionType.Reading);
		}
	}

	@Override
	public ReportGenerator findReportGeneratorByProjectOrDomainAndName(
			ProjectOrDomain projectOrDomain, String name) throws EntityException {
		try {
			// TODO: use named query so it can be configured externally
			Query query = getEntityManager()
					.createQuery(
							"select object(reportGenerator) from ReportGeneratorImpl as reportGenerator where reportGenerator.projectOrDomain = :projectOrDomain and reportGenerator.name = :name");
			query.setParameter("projectOrDomain", projectOrDomain);
			query.setParameter("name", name.trim());
			return (ReportGenerator) query.getSingleResult();
		} catch (NoResultException e) {
			throw NoSuchEntityException.byQuery(Scenario.class, new String[] { "project", "name" },
					new Object[] { projectOrDomain, name });
		} catch (Exception e) {
			throw convertException(e, Scenario.class, null, EntityExceptionActionType.Reading);
		}
	}

}
