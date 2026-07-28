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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.tagging.Tag;
import com.rreganjr.requel.tagging.TagImportHandler;
import com.rreganjr.requel.tagging.TagNormalizer;
import com.rreganjr.requel.tagging.TagRepository;
import com.rreganjr.requel.tagging.TagToken;
import com.rreganjr.requel.tagging.Taggable;

/**
 * JPA implementation of the {@link TagImportHandler} SPI. Runs within the import command's
 * transaction: resolves a global tag by {@code (category,value)} or find-or-creates a project-scoped
 * tag (scope derived from the taggable's owning project/domain), then attaches the taggable.
 *
 * @author ron
 */
@Component
public class TagImportHandlerImpl implements TagImportHandler {

	private final TagRepository tagRepository;

	@Autowired
	public TagImportHandlerImpl(TagRepository tagRepository) {
		this.tagRepository = tagRepository;
	}

	@Override
	public void assignImportedTag(Object taggable, String token, User createdBy) {
		if (!(taggable instanceof Taggable target)) {
			return;
		}
		TagToken parsed = TagToken.parse(token);
		if (parsed == null) {
			return;
		}
		String category = TagNormalizer.slug(parsed.category());
		String value = TagNormalizer.slug(parsed.value());
		if (value == null) {
			return;
		}

		// Reference-by-key: a matching global tag wins; otherwise find-or-create in the project.
		Tag tag = tagRepository.findTag(null, category, value);
		if (tag == null) {
			Long projectId = projectIdOf(taggable);
			tag = tagRepository.findTag(projectId, category, value);
			if (tag == null) {
				TagImpl created = new TagImpl(category, value, projectId, createdBy);
				created.setColor(parsed.color());
				tag = tagRepository.createTag(created);
			}
		}

		TagImpl managed = (TagImpl) tagRepository.get(tag);
		managed.getTaggables().add(target);
		tagRepository.merge(managed);
	}

	/**
	 * The owning project/domain id that scopes a project tag, or {@code null} (global) when it
	 * cannot be determined.
	 */
	private static Long projectIdOf(Object taggable) {
		if (taggable instanceof ProjectOrDomainEntity entity) {
			ProjectOrDomain owner = entity.getProjectOrDomain();
			return (owner != null) ? owner.getId() : null;
		}
		if (taggable instanceof ProjectOrDomain owner) {
			return owner.getId();
		}
		return null;
	}
}
