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
package com.rreganjr.requel.service.command;

import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.impl.ProjectImpl;
import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.api.dto.AssignTagInput;
import com.rreganjr.requel.service.api.dto.DeleteTagInput;
import com.rreganjr.requel.service.api.dto.EditTagInput;
import com.rreganjr.requel.service.api.dto.TagDto;
import com.rreganjr.requel.service.api.dto.UnassignTagInput;
import com.rreganjr.requel.tagging.Tag;
import com.rreganjr.requel.tagging.Taggable;
import com.rreganjr.requel.tagging.TagRepository;
import com.rreganjr.requel.tagging.command.AssignTagCommand;
import com.rreganjr.requel.tagging.command.DeleteTagCommand;
import com.rreganjr.requel.tagging.command.EditTagCommand;
import com.rreganjr.requel.tagging.command.TagCommandFactory;
import com.rreganjr.requel.tagging.command.UnassignTagCommand;
import com.rreganjr.requel.tagging.spi.TaggableTypeRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registers tag command types with the CQRS command registry at startup. Mirrors
 * {@link AnnotationCommandRegistrar}: each command is wired with an input applicator that
 * resolves the target entities (via {@link TaggableTypeRegistry}) and the project scope,
 * plus a result extractor producing a {@link TagDto}.
 */
@Component
public class TagCommandRegistrar {

    private static final Logger log = LoggerFactory.getLogger(TagCommandRegistrar.class);

    private final TagCommandFactory factory;
    private final CommandRegistry registry;
    private final TaggableTypeRegistry taggableTypeRegistry;
    private final EntityManager entityManager;
    private final TagRepository tagRepository;
    private final ProjectRepository projectRepository;

    public TagCommandRegistrar(TagCommandFactory factory,
                               CommandRegistry registry,
                               TaggableTypeRegistry taggableTypeRegistry,
                               EntityManager entityManager,
                               TagRepository tagRepository,
                               ProjectRepository projectRepository) {
        this.factory = factory;
        this.registry = registry;
        this.taggableTypeRegistry = taggableTypeRegistry;
        this.entityManager = entityManager;
        this.tagRepository = tagRepository;
        this.projectRepository = projectRepository;
    }

    @PostConstruct
    void registerCommands() {
        registry.register("EditTag", EditTagInput.class,
                factory::newEditTagCommand,
                (cmd, input) -> {
                    EditTagCommand c = (EditTagCommand) cmd;
                    EditTagInput i = (EditTagInput) input;
                    c.setProjectScope(resolveProjectByName(i.projectName()));
                    c.setCategory(i.category());
                    c.setValue(i.value());
                    c.setColor(i.color());
                    if (i.tagId() != null) {
                        c.setTag(tagRepository.findTagById(i.tagId()));
                    }
                },
                null,
                cmd -> toTagDto(((EditTagCommand) cmd).getTag()));

        registry.register("DeleteTag", DeleteTagInput.class,
                factory::newDeleteTagCommand,
                (cmd, input) -> {
                    DeleteTagCommand c = (DeleteTagCommand) cmd;
                    DeleteTagInput i = (DeleteTagInput) input;
                    Tag tag = tagRepository.findTagById(i.tagId());
                    c.setTag(tag);
                    c.setProjectScope(resolveProject(tag != null ? tag.getProjectId() : null));
                });

        registry.register("AssignTag", AssignTagInput.class,
                factory::newAssignTagCommand,
                (cmd, input) -> {
                    AssignTagCommand c = (AssignTagCommand) cmd;
                    AssignTagInput i = (AssignTagInput) input;
                    Tag tag = tagRepository.findTagById(i.tagId());
                    if (tag == null) {
                        throw new IllegalArgumentException("Tag not found: " + i.tagId());
                    }
                    Taggable taggable = loadTaggable(i.entityType(), i.entityId());
                    c.setTag(tag);
                    c.setTaggable(taggable);
                    c.setProjectScope(projectScopeOf(taggable));
                },
                null,
                cmd -> toTagDto(((AssignTagCommand) cmd).getTag()));

        registry.register("UnassignTag", UnassignTagInput.class,
                factory::newUnassignTagCommand,
                (cmd, input) -> {
                    UnassignTagCommand c = (UnassignTagCommand) cmd;
                    UnassignTagInput i = (UnassignTagInput) input;
                    Tag tag = tagRepository.findTagById(i.tagId());
                    if (tag == null) {
                        throw new IllegalArgumentException("Tag not found: " + i.tagId());
                    }
                    Taggable taggable = loadTaggable(i.entityType(), i.entityId());
                    c.setTag(tag);
                    c.setTaggable(taggable);
                    c.setProjectScope(projectScopeOf(taggable));
                });

        log.info("Registered {} tag command types", 4);
    }

    private Object resolveProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return entityManager.find(ProjectImpl.class, projectId);
    }

    private Object resolveProjectByName(String projectName) {
        if ((projectName == null) || projectName.isBlank()) {
            return null;
        }
        return projectRepository.findProjectByName(projectName);
    }

    private Object projectScopeOf(Taggable taggable) {
        if (taggable instanceof ProjectOrDomainEntity entity) {
            return entity.getProjectOrDomain();
        }
        if (taggable instanceof Project) {
            return taggable;
        }
        return null;
    }

    private Taggable loadTaggable(String entityType, Long entityId) {
        Class<? extends Taggable> entityClass = taggableTypeRegistry
                .resolveEntityType(entityType)
                .orElseThrow(() -> new IllegalArgumentException("Unknown entity type: " + entityType));
        Taggable entity = entityManager.find(entityClass, entityId);
        if (entity == null) {
            throw new IllegalArgumentException("Entity not found: " + entityType + "#" + entityId);
        }
        return entity;
    }

    public static TagDto toTagDto(Tag tag) {
        if (tag == null) {
            return null;
        }
        return new TagDto(
                tag.getId(),
                tag.getVersion(),
                tag.getCategory(),
                tag.getValue(),
                tag.getProjectId(),
                tag.getColor(),
                tag.getCreatedBy() != null ? tag.getCreatedBy().getDisplayName() : null);
    }
}
