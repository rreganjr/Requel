/*
 * $Id: StakeholderPermissionsInitializer.java,v 1.15 2009/03/22 11:08:24 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.project.impl.repository.init;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import edu.harvard.fas.rregan.AbstractSystemInitializer;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.requel.annotation.Annotation;
import edu.harvard.fas.rregan.requel.project.Actor;
import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.Goal;
import edu.harvard.fas.rregan.requel.project.Project;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.ReportGenerator;
import edu.harvard.fas.rregan.requel.project.Scenario;
import edu.harvard.fas.rregan.requel.project.Stakeholder;
import edu.harvard.fas.rregan.requel.project.StakeholderPermission;
import edu.harvard.fas.rregan.requel.project.StakeholderPermissionType;
import edu.harvard.fas.rregan.requel.project.Story;
import edu.harvard.fas.rregan.requel.project.UseCase;
import edu.harvard.fas.rregan.requel.project.impl.StakeholderPermissionImpl;

/**
 * @author ron
 */
@Component("stakeholderPermissionsInitializer")
@Scope("prototype")
public class StakeholderPermissionsInitializer extends AbstractSystemInitializer {

	private final ProjectRepository projectRepository;

	/**
	 * @param projectRepository
	 */
	@Autowired
	public StakeholderPermissionsInitializer(ProjectRepository projectRepository) {
		super(10);
		this.projectRepository = projectRepository;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void initialize() {
		log.debug("update stakeholder permissions...");
		for (StakeholderPermission permission : getPermissionTypes()) {
			try {
				permission = projectRepository.findStakeholderPermission(
						permission.getEntityType(), permission.getPermissionType());
				log.debug(permission + " is already persistent.");
			} catch (EntityException e) {
				log.debug("creating: " + permission);
				permission = projectRepository.persist(permission);
			}
		}
	}

	private Collection<StakeholderPermission> getPermissionTypes() {
		Collection<StakeholderPermission> entityTypes = new ArrayList<StakeholderPermission>();
		entityTypes
				.add(new StakeholderPermissionImpl(Project.class, StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(Project.class,
				StakeholderPermissionType.Grant));

		entityTypes.add(new StakeholderPermissionImpl(Annotation.class,
				StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(Annotation.class,
				StakeholderPermissionType.Grant));
		entityTypes.add(new StakeholderPermissionImpl(Annotation.class,
				StakeholderPermissionType.Delete));
		entityTypes.add(new StakeholderPermissionImpl(Goal.class, StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(Goal.class, StakeholderPermissionType.Grant));
		entityTypes
				.add(new StakeholderPermissionImpl(Goal.class, StakeholderPermissionType.Delete));
		entityTypes.add(new StakeholderPermissionImpl(Actor.class, StakeholderPermissionType.Edit));
		entityTypes
				.add(new StakeholderPermissionImpl(Actor.class, StakeholderPermissionType.Grant));
		entityTypes
				.add(new StakeholderPermissionImpl(Actor.class, StakeholderPermissionType.Delete));
		entityTypes.add(new StakeholderPermissionImpl(Stakeholder.class,
				StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(Stakeholder.class,
				StakeholderPermissionType.Grant));
		entityTypes.add(new StakeholderPermissionImpl(Stakeholder.class,
				StakeholderPermissionType.Delete));
		entityTypes.add(new StakeholderPermissionImpl(GlossaryTerm.class,
				StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(GlossaryTerm.class,
				StakeholderPermissionType.Grant));
		entityTypes.add(new StakeholderPermissionImpl(GlossaryTerm.class,
				StakeholderPermissionType.Delete));
		entityTypes.add(new StakeholderPermissionImpl(Story.class, StakeholderPermissionType.Edit));
		entityTypes
				.add(new StakeholderPermissionImpl(Story.class, StakeholderPermissionType.Grant));
		entityTypes
				.add(new StakeholderPermissionImpl(Story.class, StakeholderPermissionType.Delete));

		entityTypes
				.add(new StakeholderPermissionImpl(UseCase.class, StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(UseCase.class,
				StakeholderPermissionType.Grant));
		entityTypes.add(new StakeholderPermissionImpl(UseCase.class,
				StakeholderPermissionType.Delete));

		entityTypes.add(new StakeholderPermissionImpl(Scenario.class,
				StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(Scenario.class,
				StakeholderPermissionType.Grant));
		entityTypes.add(new StakeholderPermissionImpl(Scenario.class,
				StakeholderPermissionType.Delete));

		entityTypes.add(new StakeholderPermissionImpl(ReportGenerator.class,
				StakeholderPermissionType.Edit));
		entityTypes.add(new StakeholderPermissionImpl(ReportGenerator.class,
				StakeholderPermissionType.Grant));
		entityTypes.add(new StakeholderPermissionImpl(ReportGenerator.class,
				StakeholderPermissionType.Delete));

		return entityTypes;
	}
}
