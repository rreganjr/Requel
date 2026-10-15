/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.project.AssistantDefinition;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectAssistantDefinitions;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.StakeholderAuthorizationChecker;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.service.api.dto.ErrorResponse;
import com.rreganjr.requel.service.api.dto.ProjectDefinitionDto;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;

/**
 * Issue #264: a project's assistant definitions, read for the authoring page. Both reads need
 * {@code AssistantDefinition[Edit]} on the project, like the writes (a definition's instructions
 * are its author's work, not project content every stakeholder reads). Writes are commands.
 *
 * <ul>
 * <li>{@code GET /api/projects/{name}/definitions}: every definition in effect in the project,
 * by task type then key, each with its switch (#268).</li>
 * <li>{@code GET /api/projects/{name}/definitions/{key}}: one of them, with the bundled
 * baseline when its key is bundled.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectDefinitionsController {

	/** One definition and, for a bundled key, the bundled baseline beside it. */
	public record DefinitionDetailDto(ProjectDefinitionDto definition,
			ProjectDefinitionDto bundled) {
	}

	private final ProjectRepository projectRepository;
	private final CurrentUserResolver currentUserResolver;
	private ProjectAssistantDefinitions definitions;
	private ProjectAssistantSettingsStore settingsStore;

	@Autowired
	public ProjectDefinitionsController(ProjectRepository projectRepository,
			CurrentUserResolver currentUserResolver) {
		this.projectRepository = projectRepository;
		this.currentUserResolver = currentUserResolver;
	}

	@Autowired(required = false)
	public void setDefinitions(ProjectAssistantDefinitions definitions) {
		this.definitions = definitions;
	}

	@Autowired(required = false)
	public void setSettingsStore(ProjectAssistantSettingsStore settingsStore) {
		this.settingsStore = settingsStore;
	}

	@GetMapping("/{name}/definitions")
	@Transactional(readOnly = true)
	public ResponseEntity<?> list(@PathVariable String name) {
		try {
			Project project = requirePermission(name);
			if (definitions == null) {
				return ResponseEntity.ok(List.of());
			}
			Set<String> disabled = disabled(project);
			return ResponseEntity.ok(definitions.effective(project.getId()).stream()
					.map(view -> AssistantDefinitionDtos.toDto(view,
							!disabled.contains(view.key())))
					.toList());
		} catch (NoSuchProjectException e) {
			return ResponseEntity.notFound().build();
		} catch (AuthorizationException e) {
			return forbidden(e);
		}
	}

	@GetMapping("/{name}/definitions/{key}")
	@Transactional(readOnly = true)
	public ResponseEntity<?> get(@PathVariable String name, @PathVariable String key) {
		try {
			Project project = requirePermission(name);
			if (definitions == null) {
				return ResponseEntity.notFound().build();
			}
			var view = definitions.find(project.getId(), key);
			if (view.isEmpty()) {
				return ResponseEntity.notFound().build();
			}
			boolean enabled = !disabled(project).contains(key);
			return ResponseEntity.ok(new DefinitionDetailDto(
					AssistantDefinitionDtos.toDto(view.get(), enabled),
					definitions.bundled(key).map(b -> AssistantDefinitionDtos.toDto(b, null))
							.orElse(null)));
		} catch (NoSuchProjectException e) {
			return ResponseEntity.notFound().build();
		} catch (AuthorizationException e) {
			return forbidden(e);
		}
	}

	private Project requirePermission(String name) {
		Project project = projectRepository.findProjectByName(name);
		User user = currentUserResolver.resolve();
		StakeholderAuthorizationChecker.require(project, user, AssistantDefinition.class, "Edit");
		return project;
	}

	private Set<String> disabled(Project project) {
		return settingsStore == null ? Set.of() : settingsStore.disabledAssistants(project.getId());
	}

	private static ResponseEntity<ErrorResponse> forbidden(AuthorizationException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ErrorResponse.of("FORBIDDEN", e.getMessage()));
	}
}
