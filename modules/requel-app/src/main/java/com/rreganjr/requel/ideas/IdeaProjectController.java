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
package com.rreganjr.requel.ideas;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.assistant.ai.IdeaGenerationService;
import com.rreganjr.requel.assistant.ai.IdeaGenerationService.IdeaView;
import com.rreganjr.requel.gateway.GatewayException;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.StakeholderAuthorizationChecker;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.service.api.dto.ErrorResponse;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.service.command.IdeaFileText;

/**
 * The two writes of the idea-to-new-project flow (reading the drafts is {@link IdeaController}):
 *
 * <ul>
 * <li>{@code POST /api/ideas/new-project} (multipart: {@code projectName}, {@code text},
 * optional {@code file}): creates the project, saves the idea and queues generation. 202 with
 * the project name and the idea id.</li>
 * <li>{@code POST /api/projects/{name}/ideas/{id}/accept} (JSON {@code {"selected":[0,2]}}):
 * creates the chosen drafts as the project's requirements. Needs {@code Edit} on the project.</li>
 * </ul>
 *
 * The bean exists only when {@code requel.ai.enabled=true}.
 */
@RestController
@RequestMapping("/api")
@ConditionalOnProperty(name = "requel.ai.enabled", havingValue = "true")
public class IdeaProjectController {

	public record AcceptRequest(List<Integer> selected) {
	}

	private final IdeaProjectService service;
	private final IdeaGenerationService ideas;
	private final ProjectRepository projectRepository;
	private final CurrentUserResolver currentUserResolver;

	@Autowired
	public IdeaProjectController(IdeaProjectService service, IdeaGenerationService ideas,
			ProjectRepository projectRepository, CurrentUserResolver currentUserResolver) {
		this.service = service;
		this.ideas = ideas;
		this.projectRepository = projectRepository;
		this.currentUserResolver = currentUserResolver;
	}

	@PostMapping(value = "/ideas/new-project", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> newProject(@RequestParam String projectName,
			@RequestParam(required = false) String text,
			@RequestParam(required = false) MultipartFile file) {
		try {
			String fileText = file == null || file.isEmpty() ? null : IdeaFileText.read(file);
			String combined = IdeaProjectService.combine(text, fileText);
			return ResponseEntity.status(HttpStatus.ACCEPTED)
					.body(service.startNewProject(projectName, combined, null, fileText != null));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ErrorResponse.of("BAD_REQUEST", e.getMessage()));
		} catch (GatewayException e) {
			return gatewayFailure(e);
		}
	}

	@PostMapping("/projects/{name}/ideas/{id}/accept")
	public ResponseEntity<?> accept(@PathVariable String name, @PathVariable String id,
			@RequestBody AcceptRequest request) {
		try {
			Project project = projectRepository.findProjectByName(name);
			StakeholderAuthorizationChecker.require(project, currentUserResolver.resolve(),
					Project.class, "Edit");
			Optional<IdeaView> idea = ideas.get(id)
					.filter(view -> project.getId().equals(view.projectId()));
			if (idea.isEmpty()) {
				return ResponseEntity.notFound().build();
			}
			Set<Integer> selected = request == null || request.selected() == null ? Set.of()
					: Set.copyOf(request.selected());
			return ResponseEntity.ok(service.accept(name, idea.get(), selected));
		} catch (NoSuchProjectException e) {
			return ResponseEntity.notFound().build();
		} catch (AuthorizationException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(ErrorResponse.of("FORBIDDEN", e.getMessage()));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(ErrorResponse.of("NOT_DRAFTED", e.getMessage()));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ErrorResponse.of("BAD_REQUEST", e.getMessage()));
		} catch (GatewayException e) {
			return gatewayFailure(e);
		}
	}

	private static ResponseEntity<ErrorResponse> gatewayFailure(GatewayException e) {
		HttpStatus status = switch (e.getKind()) {
		case UNAUTHORIZED -> HttpStatus.FORBIDDEN;
		case INVALID_INPUT, NOT_ALLOWED -> HttpStatus.BAD_REQUEST;
		case NOT_FOUND -> HttpStatus.NOT_FOUND;
		default -> HttpStatus.CONFLICT;
		};
		return ResponseEntity.status(status).body(ErrorResponse.of(status.name(), e.getMessage()));
	}
}
