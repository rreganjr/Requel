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

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.validator.EntityValidationException;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.tagging.Tag;
import com.rreganjr.requel.tagging.TagCategory;
import com.rreganjr.requel.tagging.Taggable;
import com.rreganjr.requel.tagging.TagRepository;
import com.rreganjr.requel.tagging.command.AssignTagCommand;
import com.rreganjr.requel.tagging.command.TagCommandFactory;
import com.rreganjr.requel.tagging.impl.TagImpl;
import com.rreganjr.requel.tagging.spi.TaggableTypeRegistry;

/**
 * Assign a tag to a taggable entity (idempotent — the assignment set ignores duplicates).
 *
 * @author ron
 */
@Controller("assignTagCommand")
@Scope("prototype")
public class AssignTagCommandImpl extends AbstractTagCommand implements AssignTagCommand {

	private Taggable taggable;
	private final TaggableTypeRegistry taggableTypeRegistry;

	@Autowired
	public AssignTagCommandImpl(CommandHandler commandHandler, TagCommandFactory tagCommandFactory,
			TagRepository repository, TaggableTypeRegistry taggableTypeRegistry) {
		super(commandHandler, tagCommandFactory, repository);
		this.taggableTypeRegistry = taggableTypeRegistry;
	}

	@Override
	public void setTaggable(Taggable taggable) {
		this.taggable = taggable;
	}

	@Override
	protected String stakeholderPermissionType() {
		return "Edit";
	}

	@Override
	public void execute() {
		TagImpl tagImpl = (TagImpl) getRepository().get(getTag());
		Taggable managedTaggable = getRepository().get(taggable);

		applyCategoryRules(tagImpl, managedTaggable);

		tagImpl.getTaggables().add(managedTaggable);
		getRepository().merge(tagImpl);
		setTag(tagImpl);
	}

	/**
	 * Enforce the tag's typed-category rules (Phase 6): reject an entity type the category does not
	 * allow, and for an exclusive category detach any tag already on the entity in the same category
	 * (replace-on-exclusive). No-op when the tag's category has no governing {@link TagCategory}.
	 */
	private void applyCategoryRules(TagImpl tag, Taggable managedTaggable) {
		String categoryName = tag.getCategory();
		if (categoryName == null) {
			return;
		}
		TagCategory category = getTagRepository().findCategory(tag.getProjectId(), categoryName);
		if (category == null) {
			return;
		}
		String discriminator = taggableTypeRegistry
				.resolveDiscriminator(ClassUtils.getUserClass(managedTaggable)).orElse(null);

		if (!category.getAllowedEntityTypes().isEmpty()
				&& ((discriminator == null) || !category.getAllowedEntityTypes().contains(discriminator))) {
			throw EntityValidationException.validationFailed(Tag.class, "entityType",
					"Category '" + categoryName + "' cannot be assigned to "
							+ (discriminator != null ? discriminator : "this entity type") + ".");
		}

		if (category.isExclusive() && (discriminator != null)) {
			Long entityId = entityIdOf(managedTaggable);
			if (entityId != null) {
				// Flush so already-assigned tags in this transaction are visible to the native
				// tag_taggable query, then detach same-category tags from this entity.
				getRepository().flush();
				for (Tag existing : getTagRepository().findTagsOnEntity(discriminator, entityId)) {
					if (categoryName.equals(existing.getCategory())
							&& !existing.getId().equals(tag.getId())) {
						TagImpl other = (TagImpl) getRepository().get(existing);
						other.getTaggables().removeIf(t -> Objects.equals(t, managedTaggable));
						getRepository().merge(other);
					}
				}
			}
		}
	}

	private static Long entityIdOf(Taggable taggable) {
		if (taggable instanceof ProjectOrDomainEntity entity) {
			return entity.getId();
		}
		if (taggable instanceof ProjectOrDomain pod) {
			return pod.getId();
		}
		return null;
	}
}

