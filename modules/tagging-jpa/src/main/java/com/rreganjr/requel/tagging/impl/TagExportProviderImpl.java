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
package com.rreganjr.requel.tagging.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.tagging.Tag;
import com.rreganjr.requel.tagging.TagExportAssignment;
import com.rreganjr.requel.tagging.TagExportProvider;
import com.rreganjr.requel.tagging.TagRepository;
import com.rreganjr.requel.tagging.TagToken;
import com.rreganjr.requel.tagging.spi.TaggableTypeRegistry;

/**
 * JPA implementation of {@link TagExportProvider}: walks the project and its entities, and for each
 * taggable emits a {@link TagExportAssignment} per assigned tag (global and project-scoped alike),
 * rendered as a by-name token.
 *
 * @author ron
 */
@Component
public class TagExportProviderImpl implements TagExportProvider {

	private final TagRepository tagRepository;
	private final TaggableTypeRegistry taggableTypeRegistry;

	@Autowired
	public TagExportProviderImpl(TagRepository tagRepository, TaggableTypeRegistry taggableTypeRegistry) {
		this.tagRepository = tagRepository;
		this.taggableTypeRegistry = taggableTypeRegistry;
	}

	@Override
	public List<TagExportAssignment> exportAssignmentsFor(Object project) {
		List<TagExportAssignment> assignments = new ArrayList<>();
		if (!(project instanceof ProjectOrDomain pod)) {
			return assignments;
		}
		collect(assignments, pod);
		Set<ProjectOrDomainEntity> entities = pod.getProjectEntities();
		if (entities != null) {
			for (ProjectOrDomainEntity entity : entities) {
				collect(assignments, entity);
			}
		}
		return assignments;
	}

	private void collect(List<TagExportAssignment> out, Object taggable) {
		String discriminator =
				taggableTypeRegistry.resolveDiscriminator(ClassUtils.getUserClass(taggable)).orElse(null);
		if (discriminator == null) {
			return;
		}
		Long entityId = idOf(taggable);
		if (entityId == null) {
			return;
		}
		List<Tag> tags = tagRepository.findTagsOnEntity(discriminator, entityId);
		if (tags == null) {
			return;
		}
		for (Tag tag : tags) {
			String token = new TagToken(tag.getCategory(), tag.getValue(), tag.getColor()).toToken();
			out.add(new TagExportAssignment(taggable, discriminator, token));
		}
	}

	private static Long idOf(Object taggable) {
		if (taggable instanceof ProjectOrDomainEntity entity) {
			return entity.getId();
		}
		if (taggable instanceof ProjectOrDomain pod) {
			return pod.getId();
		}
		return null;
	}
}
