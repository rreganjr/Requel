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
package com.rreganjr.requel.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.assistant.api.AnalysisRequest;
import com.rreganjr.requel.assistant.api.AssistantDispatcher;
import com.rreganjr.requel.assistant.api.EntityRef;
import com.rreganjr.requel.assistant.api.UserRef;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.user.UserRepository;

/**
 * Application-layer bridge that turns a freshly-edited domain entity into an
 * {@link AnalysisRequest} and hands it to the {@link AssistantDispatcher}. This
 * lives in {@code requel-app} (not in the project/command modules) so the
 * assistant SPI dependency stays in the application layer; the command modules
 * only expose domain objects via
 * {@link com.rreganjr.requel.project.command.AnalysisRequestSource}.
 */
@Component("analysisRequestDispatcher")
public class AnalysisRequestDispatcher {

	/**
	 * The entity types a whole-project analysis covers, in dispatch order (#268). These are the
	 * text entities the assistants' target loader resolves; report generators and stakeholders
	 * are not analyzed.
	 */
	static final List<String> PROJECT_ANALYSIS_TYPES = List.of("Goal", "Story", "Actor",
			"UseCase", "Scenario", "Step", "GlossaryTerm");

	private final AssistantDispatcher assistantDispatcher;
	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	private final TransactionTemplate readTransaction;

	@Autowired
	public AnalysisRequestDispatcher(AssistantDispatcher assistantDispatcher,
			UserRepository userRepository, ProjectRepository projectRepository,
			PlatformTransactionManager transactionManager) {
		this.assistantDispatcher = Objects.requireNonNull(assistantDispatcher,
				"assistantDispatcher");
		this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
		this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
		this.readTransaction = new TransactionTemplate(
				Objects.requireNonNull(transactionManager, "transactionManager"));
		this.readTransaction.setReadOnly(true);
	}

	/**
	 * Dispatch analysis of {@code target} on behalf of {@code triggeringUser}.
	 * No-op when either is {@code null}.
	 */
	public void dispatch(ProjectOrDomainEntity target, User triggeringUser) {
		dispatch(target, triggeringUser, null);
	}

	/**
	 * Dispatch analysis of {@code target} for a specific {@code taskType} (e.g.
	 * {@code "REQUIREMENTS_REVIEW"} for a manual AI review). A {@code null} task type is the
	 * ordinary post-edit analysis. No-op when target or user is {@code null}.
	 */
	public void dispatch(ProjectOrDomainEntity target, User triggeringUser, String taskType) {
		if (target == null || triggeringUser == null) {
			return;
		}
		EntityRef targetRef = EntityRef.of(target.getProjectOrDomainEntityInterface().getSimpleName(),
				target.getId());
		ProjectOrDomain projectOrDomain = target.getProjectOrDomain();
		EntityRef projectRef = projectOrDomain != null
				? EntityRef.of("Project", projectOrDomain.getId())
				: null;
		UserRef triggeringUserRef = new UserRef(triggeringUser.getId(), triggeringUser.getUsername());
		AnalysisRequest request = new AnalysisRequest(targetRef, projectRef, triggeringUserRef,
				assistantUserRef(), taskType, Locale.getDefault(), Map.of());
		assistantDispatcher.dispatch(request);
	}

	/**
	 * Dispatch analysis of every text entity in {@code project} on behalf of
	 * {@code triggeringUser} (#268): import and the explicit re-run. The entities are listed in
	 * one read-only transaction, then handed to {@link AssistantDispatcher#dispatchAll} outside
	 * it, so the runs are queued (and may start) only after the read has finished. No-op when
	 * either argument is {@code null}.
	 *
	 * @return the number of analysis requests dispatched.
	 */
	public int dispatchProject(Project project, User triggeringUser) {
		if (project == null || project.getId() == null || triggeringUser == null) {
			return 0;
		}
		List<EntityRef> targets = readTransaction.execute(status -> projectTargets(project));
		if (targets == null || targets.isEmpty()) {
			return 0;
		}
		EntityRef projectRef = EntityRef.of("Project", project.getId());
		UserRef triggeringUserRef = new UserRef(triggeringUser.getId(), triggeringUser.getUsername());
		UserRef assistantUserRef = assistantUserRef();
		List<AnalysisRequest> requests = new ArrayList<>(targets.size());
		for (EntityRef target : targets) {
			requests.add(new AnalysisRequest(target, projectRef, triggeringUserRef, assistantUserRef,
					null, Locale.getDefault(), Map.of()));
		}
		assistantDispatcher.dispatchAll(requests);
		return requests.size();
	}

	/** The project's analyzable entities, deduplicated, ordered by type then id. */
	private List<EntityRef> projectTargets(Project project) {
		Project loaded = projectRepository.get(project);
		List<ProjectOrDomainEntity> all = new ArrayList<>();
		if (loaded.getProjectEntities() != null) {
			all.addAll(loaded.getProjectEntities());
		}
		// Steps are not project entities; scenarios can come back from both lists.
		all.addAll(projectRepository.findStepsByProjectOrDomain(loaded));
		Map<String, EntityRef> byKey = new LinkedHashMap<>();
		for (ProjectOrDomainEntity entity : all) {
			String type = entity.getProjectOrDomainEntityInterface().getSimpleName();
			if (PROJECT_ANALYSIS_TYPES.contains(type) && entity.getId() != null) {
				byKey.putIfAbsent(type + ":" + entity.getId(), EntityRef.of(type, entity.getId()));
			}
		}
		List<EntityRef> targets = new ArrayList<>(byKey.values());
		targets.sort(Comparator
				.comparingInt((EntityRef ref) -> PROJECT_ANALYSIS_TYPES.indexOf(ref.entityType()))
				.thenComparing(EntityRef::entityId));
		return targets;
	}

	private UserRef assistantUserRef() {
		User assistant = userRepository.findUserByUsername("assistant");
		return new UserRef(assistant.getId(), assistant.getUsername());
	}
}
