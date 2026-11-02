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
package com.rreganjr.requel.service.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.service.api.dto.TagCategoryDto;
import com.rreganjr.requel.service.api.dto.TagDto;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.service.command.TagCommandRegistrar;
import com.rreganjr.requel.tagging.Tag;
import com.rreganjr.requel.tagging.TagRepository;
import com.rreganjr.requel.tagging.Taggable;
import com.rreganjr.requel.tagging.spi.TaggableTypeRegistry;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import com.rreganjr.platform.exception.NoSuchEntityException;

import jakarta.persistence.EntityManager;

/**
 * Read endpoints for tags: tags in a project (+ global), tags on an entity, entities with
 * a tag, and the distinct categories used for autocomplete.
 *
 * <p>
 * Issue #447: every read checks project read access ({@link ProjectReadAccess}): a system
 * administrator reads every project, anyone else the projects they are a user stakeholder on.
 * <ul>
 * <li>A named project that does not exist is 404; one the caller cannot read is 403. With no
 * project name the routes return global tags and categories only.</li>
 * <li>An entity or tag the caller cannot read is 404, the same as one that does not exist, so
 * the id-based routes tell a non-member nothing about other projects.</li>
 * <li>A global tag lists only the entities in projects the caller can read.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/tags")
public class TagQueryController {

    private final TagRepository tagRepository;
    private final TaggableTypeRegistry taggableTypeRegistry;
    private final ProjectRepository projectRepository;
    private final EntityManager entityManager;
    private final CurrentUserResolver currentUserResolver;

    public TagQueryController(TagRepository tagRepository,
                              TaggableTypeRegistry taggableTypeRegistry,
                              ProjectRepository projectRepository,
                              EntityManager entityManager,
                              CurrentUserResolver currentUserResolver) {
        this.tagRepository = tagRepository;
        this.taggableTypeRegistry = taggableTypeRegistry;
        this.projectRepository = projectRepository;
        this.entityManager = entityManager;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * GET /api/tags?projectName={name} — the project-scoped tags plus global tags. Omit
     * {@code projectName} for global tags only.
     */
    @GetMapping
    public ResponseEntity<List<TagDto>> getTagsForProject(
            @RequestParam(required = false) String projectName) {
        ProjectScope scope = projectScope(projectName);
        if (scope.refused() != null) {
            return ResponseEntity.status(scope.refused()).build();
        }
        List<TagDto> tags = tagRepository.findTagsForProject(scope.projectId()).stream()
                .map(TagCommandRegistrar::toTagDto)
                .toList();
        return ResponseEntity.ok(tags);
    }

    /**
     * GET /api/tags/on-entity?entityType={type}&entityId={id} — tags assigned to an entity. An
     * unknown type is 400; an entity that does not exist, or is in a project the caller cannot
     * read, is 404.
     */
    @GetMapping("/on-entity")
    public ResponseEntity<List<TagDto>> getTagsOnEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId) {
        Class<? extends Taggable> entityClass = taggableTypeRegistry.resolveEntityType(entityType)
                .orElseThrow(() -> new IllegalArgumentException("Unknown entity type: " + entityType));
        Taggable taggable = entityManager.find(entityClass, entityId);
        if ((taggable == null)
                || !ProjectReadAccess.canRead(ProjectReadAccess.projectOfTagged(taggable),
                        currentUserResolver.resolve())) {
            return ResponseEntity.notFound().build();
        }
        List<TagDto> tags = tagRepository.findTagsOnEntity(entityType, entityId).stream()
                .map(TagCommandRegistrar::toTagDto)
                .toList();
        return ResponseEntity.ok(tags);
    }

    /**
     * GET /api/tags/categories?projectName={name} — distinct categories for autocomplete.
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories(
            @RequestParam(required = false) String projectName) {
        ProjectScope scope = projectScope(projectName);
        if (scope.refused() != null) {
            return ResponseEntity.status(scope.refused()).build();
        }
        return ResponseEntity.ok(tagRepository.findDistinctCategories(scope.projectId()));
    }

    /**
     * GET /api/tag-categories?projectName={name} — the typed categories in scope (project + global),
     * with their rules, for admin management and client-side enforcement.
     */
    @GetMapping("/categories/typed")
    public ResponseEntity<List<TagCategoryDto>> getTypedCategories(
            @RequestParam(required = false) String projectName) {
        ProjectScope scope = projectScope(projectName);
        if (scope.refused() != null) {
            return ResponseEntity.status(scope.refused()).build();
        }
        List<TagCategoryDto> categories =
                tagRepository.findCategoriesForProject(scope.projectId()).stream()
                        .map(TagCommandRegistrar::toTagCategoryDto)
                        .toList();
        return ResponseEntity.ok(categories);
    }

    /**
     * The project a read is scoped to: a null id for no project name (global only), else the named
     * project's id, or the status to refuse with (404 for no such project, 403 for one the caller
     * cannot read).
     */
    private ProjectScope projectScope(String projectName) {
        if ((projectName == null) || projectName.isBlank()) {
            return new ProjectScope(null, null);
        }
        Project project;
        try {
            project = projectRepository.findProjectByName(projectName);
        } catch (NoSuchProjectException e) {
            return new ProjectScope(null, HttpStatus.NOT_FOUND);
        }
        if (!ProjectReadAccess.canRead(project, currentUserResolver.resolve())) {
            return new ProjectScope(null, HttpStatus.FORBIDDEN);
        }
        return new ProjectScope(project.getId(), null);
    }

    private record ProjectScope(Long projectId, HttpStatus refused) {
    }

    /**
     * GET /api/tags/{tagId}/entities — the entities a tag is assigned to, as
     * {@code {entityType, entityId}} references. A tag that does not exist, or a project tag in a
     * project the caller cannot read, is 404. The list holds only entities in projects the caller
     * can read; an entity whose project cannot be found is listed for a system administrator only.
     */
    @GetMapping("/{tagId}/entities")
    public ResponseEntity<List<Map<String, Object>>> getEntitiesWithTag(
            @PathVariable Long tagId) {
        Tag tag = tagRepository.findTagById(tagId);
        if (tag == null) {
            return ResponseEntity.notFound().build();
        }
        ReadableProjects readable = new ReadableProjects(currentUserResolver.resolve());
        if ((tag.getProjectId() != null) && !readable.canRead(tag.getProjectId())) {
            return ResponseEntity.notFound().build();
        }
        List<Map<String, Object>> refs = new ArrayList<>();
        for (Taggable taggable : tag.getTaggables()) {
            Class<?> userClass = ClassUtils.getUserClass(taggable);
            String discriminator = taggableTypeRegistry.resolveDiscriminator(userClass).orElse(null);
            Long entityId = idOf(taggable);
            if ((discriminator != null) && (entityId != null)
                    && readable.canRead(ProjectReadAccess.projectOfTagged(taggable))) {
                Map<String, Object> ref = new LinkedHashMap<>();
                ref.put("entityType", discriminator);
                ref.put("entityId", entityId);
                refs.add(ref);
            }
        }
        return ResponseEntity.ok(refs);
    }

    /**
     * The caller's read access, decided once per project for one request: a global tag can be
     * on many entities in a few projects.
     */
    private final class ReadableProjects {
        private final User caller;
        private final boolean admin;
        private final Map<Long, Boolean> byProjectId = new HashMap<>();

        ReadableProjects(User caller) {
            this.caller = caller;
            this.admin = (caller != null) && caller.hasRole(SystemAdminUserRole.class);
        }

        boolean canRead(Long projectId) {
            return byProjectId.computeIfAbsent(projectId, id -> {
                try {
                    return ProjectReadAccess.canRead(projectRepository.findById(Project.class, id),
                            caller);
                } catch (NoSuchEntityException e) {
                    return admin;
                }
            });
        }

        /** A null project (one this cannot find) is readable by a system administrator only. */
        boolean canRead(Project project) {
            if ((project == null) || (project.getId() == null)) {
                return admin;
            }
            return byProjectId.computeIfAbsent(project.getId(),
                    id -> ProjectReadAccess.canRead(project, caller));
        }
    }

    private static Long idOf(Taggable taggable) {
        try {
            Object id = taggable.getClass().getMethod("getId").invoke(taggable);
            return (id instanceof Long l) ? l : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
