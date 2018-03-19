/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * 
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
package com.rreganjr.requel.project.impl.repository.init;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.initializer.AbstractSystemInitializer;
import com.rreganjr.EntityException;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.StakeholderPermission;
import com.rreganjr.requel.project.StakeholderPermissionType;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.impl.StakeholderPermissionImpl;

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
