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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.identity.User;
import com.rreganjr.validator.EntityValidationException;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.tagging.Tag;
import com.rreganjr.requel.tagging.TagNormalizer;
import com.rreganjr.requel.tagging.TagRepository;
import com.rreganjr.requel.tagging.command.EditTagCommand;
import com.rreganjr.requel.tagging.command.TagCommandFactory;
import com.rreganjr.requel.tagging.impl.TagImpl;

/**
 * Create a new tag or edit an existing one. Category/value are normalized to slugs on
 * write and {@code (project_id, category, value)} uniqueness is enforced.
 *
 * @author ron
 */
@Controller("editTagCommand")
@Scope("prototype")
public class EditTagCommandImpl extends AbstractTagCommand implements EditTagCommand {

	private String category;
	private String value;
	private String description;
	private String color;

	@Autowired
	public EditTagCommandImpl(CommandHandler commandHandler, TagCommandFactory tagCommandFactory,
			TagRepository repository) {
		super(commandHandler, tagCommandFactory, repository);
	}

	@Override
	public void setCategory(String category) {
		this.category = category;
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void setColor(String color) {
		this.color = color;
	}

	@Override
	public Tag getTag() {
		return super.getTag();
	}

	@Override
	protected String stakeholderPermissionType() {
		return "Edit";
	}

	@Override
	public void execute() {
		String normalizedCategory = TagNormalizer.slug(category);
		String normalizedValue = TagNormalizer.slug(value);
		if (normalizedValue == null) {
			throw EntityValidationException.emptyRequiredProperty(Tag.class, getTag(), "value",
					EntityExceptionActionType.Updating);
		}

		Project project = getProject();
		Long projectId = (project != null) ? project.getId() : null;
		User editedBy = getRepository().get(getEditedBy());

		TagImpl tagImpl = (TagImpl) getTag();
		if (tagImpl == null) {
			// Create — reuse an existing tag with the same normalized identity in scope.
			Tag existing = getTagRepository().findTag(projectId, normalizedCategory, normalizedValue);
			if (existing != null) {
				setTag(existing);
				return;
			}
			tagImpl = new TagImpl(normalizedCategory, normalizedValue, projectId, editedBy);
			tagImpl.setDescriptionText(description);
			tagImpl.setColor(color);
			tagImpl = getRepository().persist(tagImpl);
		} else {
			// Update — guard the uniqueness of the (scope, category, value) identity.
			Tag conflict = getTagRepository().findTag(projectId, normalizedCategory, normalizedValue);
			if ((conflict != null) && !conflict.getId().equals(tagImpl.getId())) {
				throw EntityValidationException.validationFailed(Tag.class, "value",
						"A tag with that category and value already exists in this scope.");
			}
			tagImpl.setCategory(normalizedCategory);
			tagImpl.setValue(normalizedValue);
			tagImpl.setDescriptionText(description);
			tagImpl.setColor(color);
			tagImpl = getRepository().merge(tagImpl);
		}
		setTag(tagImpl);
	}
}
