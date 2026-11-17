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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.assistant.ai.IdeaGenerationService;
import com.rreganjr.requel.assistant.ai.IdeaGenerationService.IdeaView;
import com.rreganjr.requel.assistant.ai.IdeaToRequirementsService.Idea;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.StakeholderAuthorizationChecker;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.service.api.dto.ErrorResponse;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;

/**
 * A project's ideas and the requirement drafts generated from them, read for the ideas page.
 * Generation runs in the background ({@link IdeaGenerationService}); the page polls
 * {@code GET .../ideas/{id}} while the idea is {@code PENDING} or {@code GENERATING}.
 *
 * <ul>
 * <li>{@code GET /api/projects/{name}/ideas}: the project's ideas, newest first, without drafts.</li>
 * <li>{@code GET /api/projects/{name}/ideas/{id}}: one idea with its drafts, open questions
 * ({@code messages}) and what validation dropped or adjusted ({@code problems}).</li>
 * <li>{@code POST /api/projects/{name}/ideas}: save an idea and queue its generation.</li>
 * <li>{@code POST /api/projects/{name}/ideas/{id}/retry}: generate again a failed or skipped idea.</li>
 * </ul>
 *
 * Reading needs {@code View} on the project, writing {@code Edit}. The bean exists only when
 * {@code requel.ai.enabled=true}, like {@link IdeaGenerationService}.
 */
@RestController
@RequestMapping("/api/projects")
@ConditionalOnProperty(name = "requel.ai.enabled", havingValue = "true")
public class IdeaController {

	/** The text of an idea, and optionally the project names it should not propose again. */
	public record SubmitIdeaRequest(String ideaText, List<String> existingGoals,
			List<String> existingActors, List<String> existingGlossaryTerms) {
	}

	private final ProjectRepository projectRepository;
	private final CurrentUserResolver currentUserResolver;
	private final IdeaGenerationService ideas;

	@Autowired
	public IdeaController(ProjectRepository projectRepository,
			CurrentUserResolver currentUserResolver, IdeaGenerationService ideas) {
		this.projectRepository = projectRepository;
		this.currentUserResolver = currentUserResolver;
		this.ideas = ideas;
	}

	@GetMapping("/{name}/ideas")
	@Transactional(readOnly = true)
	public ResponseEntity<?> list(@PathVariable String name) {
		try {
			Project project = require(name, "View").project();
			return ResponseEntity.ok(ideas.list(project.getId()));
		} catch (NoSuchProjectException e) {
			return ResponseEntity.notFound().build();
		} catch (AuthorizationException e) {
			return forbidden(e);
		}
	}

	@GetMapping("/{name}/ideas/{id}")
	@Transactional(readOnly = true)
	public ResponseEntity<?> get(@PathVariable String name, @PathVariable String id) {
		try {
			Project project = require(name, "View").project();
			Optional<IdeaView> idea = inProject(project, id);
			return idea.<ResponseEntity<?>>map(ResponseEntity::ok)
					.orElseGet(() -> ResponseEntity.notFound().build());
		} catch (NoSuchProjectException e) {
			return ResponseEntity.notFound().build();
		} catch (AuthorizationException e) {
			return forbidden(e);
		}
	}

	@PostMapping("/{name}/ideas")
	@Transactional
	public ResponseEntity<?> submit(@PathVariable String name,
			@RequestBody SubmitIdeaRequest request) {
		try {
			Caller caller = require(name, "Edit");
			String id = ideas.submit(caller.project().getId(),
					new UserRef(caller.user().getId(), caller.user().getUsername()),
					new Idea(request.ideaText(), request.existingGoals(),
							request.existingActors(), request.existingGlossaryTerms()));
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("id", id));
		} catch (NoSuchProjectException e) {
			return ResponseEntity.notFound().build();
		} catch (AuthorizationException e) {
			return forbidden(e);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(ErrorResponse.of("BAD_REQUEST", e.getMessage()));
		}
	}

	@PostMapping("/{name}/ideas/{id}/retry")
	@Transactional
	public ResponseEntity<?> retry(@PathVariable String name, @PathVariable String id) {
		try {
			Project project = require(name, "Edit").project();
			if (inProject(project, id).isEmpty()) {
				return ResponseEntity.notFound().build();
			}
			ideas.retry(id);
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("id", id));
		} catch (NoSuchProjectException e) {
			return ResponseEntity.notFound().build();
		} catch (AuthorizationException e) {
			return forbidden(e);
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(ErrorResponse.of("NOT_RETRYABLE", e.getMessage()));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}
	}

	/** The idea, only when it belongs to {@code project}: another project's id reads as missing. */
	private Optional<IdeaView> inProject(Project project, String id) {
		return ideas.get(id).filter(view -> project.getId().equals(view.projectId()));
	}

	private record Caller(Project project, User user) {
	}

	private Caller require(String name, String permission) {
		Project project = projectRepository.findProjectByName(name);
		User user = currentUserResolver.resolve();
		StakeholderAuthorizationChecker.require(project, user, Project.class, permission);
		return new Caller(project, user);
	}

	private static ResponseEntity<ErrorResponse> forbidden(AuthorizationException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ErrorResponse.of("FORBIDDEN", e.getMessage()));
	}
}
