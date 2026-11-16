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
package com.rreganjr.requel.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.assistant.ai.AiProperties;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.core.SimpleAssistantRegistry;
import com.rreganjr.requel.assistant.core.corpus.CorpusFinderAssistant;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunReadService;
import com.rreganjr.requel.command.AnalysisRequestDispatcher;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.service.api.dto.AiReviewAvailabilityDto;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;

/**
 * Issue #266: explicit corpus runs over a set of entities, a project or a goal or use case with
 * what hangs off it. {@code CANDIDATES} is "Find overlaps" (the non-AI finder);
 * {@code ANALYSIS} is the AI corpus analysis. Nothing else ever dispatches these task types, so a
 * corpus run never follows an edit.
 */
@Service
public class CorpusAnalysisService {

	/** What to run. */
	public enum Mode {
		CANDIDATES(CorpusFinderAssistant.TASK_TYPE), ANALYSIS(CORPUS_REVIEW);

		private final String taskType;

		Mode(String taskType) {
			this.taskType = taskType;
		}

		public String taskType() {
			return taskType;
		}

		static Mode parse(String value) {
			try {
				return Mode.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Unknown corpus mode: " + value
						+ " (known: CANDIDATES, ANALYSIS)");
			}
		}
	}

	/** The AI corpus analysis task. */
	public static final String CORPUS_REVIEW = "CORPUS_REVIEW";

	private static final Map<String, Class<? extends ProjectOrDomainEntity>> ROOT_TYPES = Map.of(
			"GOAL", Goal.class, "USE_CASE", UseCase.class);

	private final ProjectRepository projectRepository;
	private final CurrentUserResolver currentUserResolver;
	private final AnalysisRequestDispatcher dispatcher;
	private final AssistantRunReadService runReadService;
	private final TransactionTemplate readTransaction;
	private AiProperties aiProperties;
	private SimpleAssistantRegistry registry;

	/** Issue #486: the AI corpus analysis needs a corpus definition in effect. */
	static final String CORPUS_OFF = "AI corpus analysis is switched off for this project.";

	@Autowired
	public CorpusAnalysisService(ProjectRepository projectRepository,
			CurrentUserResolver currentUserResolver, AnalysisRequestDispatcher dispatcher,
			AssistantRunReadService runReadService, PlatformTransactionManager transactionManager) {
		this.projectRepository = projectRepository;
		this.currentUserResolver = currentUserResolver;
		this.dispatcher = dispatcher;
		this.runReadService = runReadService;
		this.readTransaction = new TransactionTemplate(transactionManager);
		this.readTransaction.setReadOnly(true);
	}

	/** Issue #486: without AI properties (AI off at build time) an AI analysis can never run. */
	@Autowired(required = false)
	public void setAiProperties(AiProperties aiProperties) {
		this.aiProperties = aiProperties;
	}

	/** Issue #486: which corpus definitions are in effect; without it, none is. */
	@Autowired(required = false)
	public void setRegistry(SimpleAssistantRegistry registry) {
		this.registry = registry;
	}

	/**
	 * Issue #486: whether an AI corpus analysis ({@code ANALYSIS}) of {@code projectId} can run,
	 * and why not: AI off on the server, the project not on the allowlist, or no corpus
	 * definition in effect after the project's switches. The project page
	 * asks this before it shows "Analyse with AI", the way the editors ask before showing
	 * "Review with AI" (#387). Same access check as {@link #request}.
	 */
	public AiReviewAvailabilityDto availability(Long projectId) {
		User user = currentUserResolver.resolve();
		readTransaction.execute(status -> root(projectId, "PROJECT", null, user));
		String reason = analysisUnavailableReason(projectId);
		return new AiReviewAvailabilityDto(reason == null, reason);
	}

	private String analysisUnavailableReason(Long projectId) {
		if (aiProperties == null || !aiProperties.isEnabled()) {
			return AiReviewService.AI_OFF;
		}
		List<String> allowlist = aiProperties.getProjectAllowlist();
		if (allowlist != null && !allowlist.isEmpty() && !allowlist.contains(String.valueOf(projectId))) {
			return AiReviewService.NOT_ALLOWLISTED;
		}
		if (registry == null || !registry.anyCorpusReview(projectId)) {
			return CORPUS_OFF;
		}
		return null;
	}

	/**
	 * Dispatch a corpus run over the set {@code set} ({@code PROJECT}, {@code GOAL} or
	 * {@code USE_CASE}; the latter two with {@code rootId}) of {@code projectId}.
	 *
	 * @throws IllegalArgumentException for an unknown set or mode, a missing root, or a root in
	 *             another project
	 * @throws com.rreganjr.platform.exception.NoSuchEntityException if the project or root does
	 *             not exist
	 * @throws AuthorizationException without access to the project
	 * @throws AiReviewUnavailableException for an AI analysis that can't run (#486)
	 */
	public void request(Long projectId, String set, Long rootId, String mode) {
		Mode parsed = Mode.parse(mode);
		User user = currentUserResolver.resolve();
		EntityRef root = readTransaction.execute(status -> root(projectId, set, rootId, user));
		if (parsed == Mode.ANALYSIS) {
			// #486: refuse an AI analysis that can't run, rather than queue one that is skipped.
			String reason = analysisUnavailableReason(projectId);
			if (reason != null) {
				throw new AiReviewUnavailableException(reason);
			}
		}
		dispatcher.dispatchCorpus(root, projectId, user, parsed.taskType());
	}

	/** The latest run of {@code mode} over the set, or empty. Same checks as {@link #request}. */
	public Optional<AssistantRunReadService.RunView> latest(Long projectId, String set,
			Long rootId, String mode) {
		Mode parsed = Mode.parse(mode);
		User user = currentUserResolver.resolve();
		EntityRef root = readTransaction.execute(status -> root(projectId, set, rootId, user));
		return runReadService.latestRun(root.entityType(), root.entityId(), parsed.taskType());
	}

	private EntityRef root(Long projectId, String set, Long rootId, User user) {
		Project project = projectRepository.findById(Project.class, projectId);
		requireProjectAccess(project, user);
		String kind = set == null ? "PROJECT" : set.trim().toUpperCase(java.util.Locale.ROOT);
		if ("PROJECT".equals(kind)) {
			return EntityRef.of("Project", project.getId());
		}
		Class<? extends ProjectOrDomainEntity> type = ROOT_TYPES.get(kind);
		if (type == null) {
			throw new IllegalArgumentException("Unknown corpus set: " + set
					+ " (known: PROJECT, GOAL, USE_CASE)");
		}
		if (rootId == null) {
			throw new IllegalArgumentException("A " + kind + " set needs rootId");
		}
		ProjectOrDomainEntity root = projectRepository.findById(type, rootId);
		if (root.getProjectOrDomain() == null
				|| !project.getId().equals(root.getProjectOrDomain().getId())) {
			throw new IllegalArgumentException(type.getSimpleName() + " " + rootId
					+ " is not in project " + projectId);
		}
		return EntityRef.of(type.getSimpleName(), root.getId());
	}

	private static void requireProjectAccess(ProjectOrDomain project, User user) {
		if (user.hasRole(SystemAdminUserRole.class)) {
			return;
		}
		for (Stakeholder stakeholder : project.getStakeholders()) {
			if (stakeholder.matchesUser(user) && stakeholder instanceof UserStakeholder) {
				return;
			}
		}
		throw new AuthorizationException("You do not have access to this project.");
	}
}
