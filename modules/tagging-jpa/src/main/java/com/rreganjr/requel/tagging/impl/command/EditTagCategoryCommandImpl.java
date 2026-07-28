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
package com.rreganjr.requel.tagging.impl.command;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.identity.User;
import com.rreganjr.validator.EntityValidationException;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.tagging.TagCategory;
import com.rreganjr.requel.tagging.TagNormalizer;
import com.rreganjr.requel.tagging.TagRepository;
import com.rreganjr.requel.tagging.command.EditTagCategoryCommand;
import com.rreganjr.requel.tagging.command.TagCommandFactory;
import com.rreganjr.requel.tagging.impl.TagCategoryImpl;

/**
 * Create or update a tag category. The name is normalized to a slug (categories are slugs, like tag
 * values). On create, an existing category with the same normalized {@code (scope, name)} is reused.
 *
 * @author ron
 */
@Controller("editTagCategoryCommand")
@Scope("prototype")
public class EditTagCategoryCommandImpl extends AbstractTagCategoryCommand implements EditTagCategoryCommand {

	private Long categoryId;
	private String name;
	private boolean exclusive;
	private String color;
	private Set<String> allowedEntityTypes = new HashSet<>();
	private Set<String> values = new HashSet<>();

	@Autowired
	public EditTagCategoryCommandImpl(CommandHandler commandHandler, TagCommandFactory tagCommandFactory,
			TagRepository repository) {
		super(commandHandler, tagCommandFactory, repository);
	}

	@Override
	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
	}

	@Override
	public void setColor(String color) {
		this.color = color;
	}

	@Override
	public void setAllowedEntityTypes(Set<String> allowedEntityTypes) {
		this.allowedEntityTypes = (allowedEntityTypes != null) ? allowedEntityTypes : new HashSet<>();
	}

	@Override
	public void setValues(Set<String> values) {
		this.values = (values != null) ? values : new HashSet<>();
	}

	@Override
	protected String stakeholderPermissionType() {
		return "Edit";
	}

	@Override
	public void execute() {
		String normalizedName = TagNormalizer.slug(name);
		if (normalizedName == null) {
			throw EntityValidationException.emptyRequiredProperty(TagCategory.class, getTagCategory(),
					"name", EntityExceptionActionType.Updating);
		}

		Project project = getProject();
		Long projectId = (project != null) ? project.getId() : null;
		User editedBy = getRepository().get(getEditedBy());

		TagCategoryImpl category;
		boolean isNew;
		if (categoryId != null) {
			category = (TagCategoryImpl) getTagRepository().findCategoryById(categoryId);
			if (category == null) {
				throw EntityValidationException.validationFailed(TagCategory.class, "id",
						"Tag category not found: " + categoryId);
			}
			isNew = false;
		} else {
			// Reuse only an exact-scope match (findCategory falls back to global; guard on projectId).
			TagCategory existing = getTagRepository().findCategory(projectId, normalizedName);
			if ((existing != null) && Objects.equals(existing.getProjectId(), projectId)) {
				category = (TagCategoryImpl) existing;
				isNew = false;
			} else {
				category = new TagCategoryImpl(normalizedName, projectId, exclusive, editedBy);
				isNew = true;
			}
		}

		category.setName(normalizedName);
		category.setExclusive(exclusive);
		category.setColor(color);
		category.getAllowedEntityTypes().clear();
		category.getAllowedEntityTypes().addAll(allowedEntityTypes);
		category.getValues().clear();
		category.getValues().addAll(normalizeValues(values));

		category = isNew ? getRepository().persist(category) : getRepository().merge(category);
		setTagCategory(category);
	}

	private static Set<String> normalizeValues(Set<String> raw) {
		Set<String> normalized = new LinkedHashSet<>();
		for (String value : raw) {
			String slug = TagNormalizer.slug(value);
			if (slug != null) {
				normalized.add(slug);
			}
		}
		return normalized;
	}
}
