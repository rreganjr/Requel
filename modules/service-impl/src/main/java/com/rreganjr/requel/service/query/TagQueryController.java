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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.service.api.dto.TagCategoryDto;
import com.rreganjr.requel.service.api.dto.TagDto;
import com.rreganjr.requel.service.command.TagCommandRegistrar;
import com.rreganjr.requel.tagging.Tag;
import com.rreganjr.requel.tagging.Taggable;
import com.rreganjr.requel.tagging.TagRepository;
import com.rreganjr.requel.tagging.spi.TaggableTypeRegistry;

/**
 * Read endpoints for tags: tags in a project (+ global), tags on an entity, entities with
 * a tag, and the distinct categories used for autocomplete.
 */
@RestController
@RequestMapping("/api/tags")
public class TagQueryController {

    private final TagRepository tagRepository;
    private final TaggableTypeRegistry taggableTypeRegistry;
    private final ProjectRepository projectRepository;

    public TagQueryController(TagRepository tagRepository,
                              TaggableTypeRegistry taggableTypeRegistry,
                              ProjectRepository projectRepository) {
        this.tagRepository = tagRepository;
        this.taggableTypeRegistry = taggableTypeRegistry;
        this.projectRepository = projectRepository;
    }

    /**
     * GET /api/tags?projectName={name} — the project-scoped tags plus global tags. Omit
     * {@code projectName} for global tags only.
     */
    @GetMapping
    public ResponseEntity<List<TagDto>> getTagsForProject(
            @RequestParam(required = false) String projectName) {
        List<TagDto> tags = tagRepository.findTagsForProject(resolveProjectId(projectName)).stream()
                .map(TagCommandRegistrar::toTagDto)
                .toList();
        return ResponseEntity.ok(tags);
    }

    /**
     * GET /api/tags/on-entity?entityType={type}&entityId={id} — tags assigned to an entity.
     */
    @GetMapping("/on-entity")
    public ResponseEntity<List<TagDto>> getTagsOnEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId) {
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
        return ResponseEntity.ok(tagRepository.findDistinctCategories(resolveProjectId(projectName)));
    }

    /**
     * GET /api/tag-categories?projectName={name} — the typed categories in scope (project + global),
     * with their rules, for admin management and client-side enforcement.
     */
    @GetMapping("/categories/typed")
    public ResponseEntity<List<TagCategoryDto>> getTypedCategories(
            @RequestParam(required = false) String projectName) {
        List<TagCategoryDto> categories =
                tagRepository.findCategoriesForProject(resolveProjectId(projectName)).stream()
                        .map(TagCommandRegistrar::toTagCategoryDto)
                        .toList();
        return ResponseEntity.ok(categories);
    }

    private Long resolveProjectId(String projectName) {
        if ((projectName == null) || projectName.isBlank()) {
            return null;
        }
        return projectRepository.findProjectByName(projectName).getId();
    }

    /**
     * GET /api/tags/{tagId}/entities — the entities a tag is assigned to, as
     * {@code {entityType, entityId}} references.
     */
    @GetMapping("/{tagId}/entities")
    public ResponseEntity<List<Map<String, Object>>> getEntitiesWithTag(
            @PathVariable Long tagId) {
        Tag tag = tagRepository.findTagById(tagId);
        if (tag == null) {
            return ResponseEntity.notFound().build();
        }
        List<Map<String, Object>> refs = new ArrayList<>();
        for (Taggable taggable : tag.getTaggables()) {
            Class<?> userClass = ClassUtils.getUserClass(taggable);
            String discriminator = taggableTypeRegistry.resolveDiscriminator(userClass).orElse(null);
            Long entityId = idOf(taggable);
            if ((discriminator != null) && (entityId != null)) {
                Map<String, Object> ref = new LinkedHashMap<>();
                ref.put("entityType", discriminator);
                ref.put("entityId", entityId);
                refs.add(ref);
            }
        }
        return ResponseEntity.ok(refs);
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
