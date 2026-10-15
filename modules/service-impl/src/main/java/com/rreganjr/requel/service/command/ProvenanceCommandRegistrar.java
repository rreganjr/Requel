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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.command.Command;
import com.rreganjr.requel.gateway.CommandPolicy;
import com.rreganjr.requel.gateway.PolicyDecision;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.SourceLinkRelation;
import com.rreganjr.requel.project.SourceLocatorType;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.AddSourceAuthorityCommand;
import com.rreganjr.requel.project.command.DeleteSourceCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditNonUserStakeholderCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.LinkSourceCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.RecordSourceCommand;
import com.rreganjr.requel.project.command.RemoveSourceAuthorityCommand;
import com.rreganjr.requel.project.command.UnlinkSourceCommand;
import com.rreganjr.requel.project.command.UpsertFromSourceCommand;
import com.rreganjr.requel.project.impl.ProvenanceEntityTypes;
import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.api.dto.AddSourceAuthorityInput;
import com.rreganjr.requel.service.api.dto.DeleteSourceInput;
import com.rreganjr.requel.service.api.dto.EntitySourceLinkDto;
import com.rreganjr.requel.service.api.dto.LinkSourceInput;
import com.rreganjr.requel.service.api.dto.RecordSourceInput;
import com.rreganjr.requel.service.api.dto.RecordSourceResultDto;
import com.rreganjr.requel.service.api.dto.RemoveSourceAuthorityInput;
import com.rreganjr.requel.service.api.dto.SourceAuthorityDto;
import com.rreganjr.requel.service.api.dto.UnlinkSourceInput;
import com.rreganjr.requel.service.api.dto.UpsertFromSourceInput;
import com.rreganjr.requel.service.api.dto.UpsertFromSourceResultDto;
import com.rreganjr.requel.service.query.ProvenanceQueryService;

import jakarta.annotation.PostConstruct;

/**
 * Registers the provenance commands (issue #272): RecordSource, LinkSource, UnlinkSource and the
 * composite UpsertFromSource, which wraps one of the entity edit commands. Issue #273 adds
 * DeleteSource and the source-authority pair, and a relation on LinkSource/UnlinkSource.
 */
@Component
public class ProvenanceCommandRegistrar {

	/**
	 * The edit commands UpsertFromSource can wrap: the entity type each writes, the name of the
	 * id field in its input, and how to read the entity back from the executed command.
	 */
	record EditTarget(Class<? extends ProjectOrDomainEntity> entityType, String idField,
			Function<Command, ProjectOrDomainEntity> entityOf) {
	}

	static final Map<String, EditTarget> EDIT_TARGETS = Map.of(
			"EditGoal", new EditTarget(Goal.class, "goalId",
					c -> ((EditGoalCommand) c).getGoal()),
			"EditStory", new EditTarget(Story.class, "storyId",
					c -> ((EditStoryCommand) c).getStory()),
			"EditActor", new EditTarget(Actor.class, "actorId",
					c -> ((EditActorCommand) c).getActor()),
			"EditUseCase", new EditTarget(UseCase.class, "useCaseId",
					c -> ((EditUseCaseCommand) c).getUseCase()),
			"EditScenario", new EditTarget(Scenario.class, "scenarioId",
					c -> ((EditScenarioCommand) c).getScenario()),
			"EditGlossaryTerm", new EditTarget(GlossaryTerm.class, "termId",
					c -> ((EditGlossaryTermCommand) c).getGlossaryTerm()),
			"EditNonUserStakeholder", new EditTarget(NonUserStakeholder.class, "stakeholderId",
					c -> ((EditNonUserStakeholderCommand) c).getStakeholder()));

	private final ProjectCommandFactory factory;
	private final ProjectRepository projectRepository;
	private final CommandRegistry registry;
	private final ObjectMapper objectMapper;
	private final ObjectProvider<ApiCommandFactory> apiCommandFactory;
	private final ObjectProvider<CommandPolicy> commandPolicy;

	public ProvenanceCommandRegistrar(ProjectCommandFactory factory,
			ProjectRepository projectRepository, CommandRegistry registry,
			ObjectMapper objectMapper, ObjectProvider<ApiCommandFactory> apiCommandFactory,
			ObjectProvider<CommandPolicy> commandPolicy) {
		this.factory = factory;
		this.projectRepository = projectRepository;
		this.registry = registry;
		this.objectMapper = objectMapper;
		this.apiCommandFactory = apiCommandFactory;
		this.commandPolicy = commandPolicy;
	}

	@PostConstruct
	void registerCommands() {
		registry.register("RecordSource", RecordSourceInput.class,
				factory::newRecordSourceCommand,
				(cmd, input) -> {
					RecordSourceCommand c = (RecordSourceCommand) cmd;
					RecordSourceInput i = (RecordSourceInput) input;
					c.setProject(projectRepository.findProjectByName(i.projectName()));
					c.setSystem(i.system());
					c.setExternalId(i.externalId());
					c.setLocatorType(locatorType(i.locatorType()));
					c.setLocator(i.locator());
					c.setTitle(i.title());
					c.setContentHash(i.contentHash());
					c.setKind(i.kind());
					c.setNote(i.note());
				},
				null,
				cmd -> {
					var recorded = ((RecordSourceCommand) cmd).getRecordedSource();
					return new RecordSourceResultDto(
							ProvenanceQueryService.toSourceDto(recorded.source()),
							recorded.created(), recorded.changed());
				});

		registry.register("LinkSource", LinkSourceInput.class,
				factory::newLinkSourceCommand,
				(cmd, input) -> {
					LinkSourceCommand c = (LinkSourceCommand) cmd;
					LinkSourceInput i = (LinkSourceInput) input;
					Project project = projectRepository.findProjectByName(i.projectName());
					c.setProject(project);
					c.setTarget(loadTarget(project, i.entityType(), i.entityId()));
					c.setSystem(i.system());
					c.setExternalId(i.externalId());
					c.setFragment(i.fragment());
					c.setFragmentText(i.fragmentText());
					c.setRelation(SourceLinkRelation.parse(i.relation()));
				},
				null,
				cmd -> {
					LinkSourceCommand c = (LinkSourceCommand) cmd;
					var link = c.getLink();
					return new EntitySourceLinkDto(link.getId(),
							ProvenanceQueryService.toSourceDto(link.getSource()),
							link.getRelation().name(), link.getTargetType(), link.getTargetId(),
							null, link.getFragment(), link.getIngestedAt(),
							link.isNotInLatestSource(), false);
				});

		registry.register("UnlinkSource", UnlinkSourceInput.class,
				factory::newUnlinkSourceCommand,
				(cmd, input) -> {
					UnlinkSourceCommand c = (UnlinkSourceCommand) cmd;
					UnlinkSourceInput i = (UnlinkSourceInput) input;
					Project project = projectRepository.findProjectByName(i.projectName());
					c.setProject(project);
					c.setTarget(loadTarget(project, i.entityType(), i.entityId()));
					c.setSystem(i.system());
					c.setExternalId(i.externalId());
					c.setFragment(i.fragment());
					c.setRelation(SourceLinkRelation.parse(i.relation()));
				},
				null,
				cmd -> Map.of("unlinked", ((UnlinkSourceCommand) cmd).isUnlinked()));

		registry.register("DeleteSource", DeleteSourceInput.class,
				factory::newDeleteSourceCommand,
				(cmd, input) -> {
					DeleteSourceCommand c = (DeleteSourceCommand) cmd;
					DeleteSourceInput i = (DeleteSourceInput) input;
					c.setProject(projectRepository.findProjectByName(i.projectName()));
					c.setSystem(i.system());
					c.setExternalId(i.externalId());
				},
				null,
				cmd -> Map.of("deleted", true, "citationsRemoved",
						((DeleteSourceCommand) cmd).getCitationsRemoved()));

		registry.register("AddSourceAuthority", AddSourceAuthorityInput.class,
				factory::newAddSourceAuthorityCommand,
				(cmd, input) -> {
					AddSourceAuthorityCommand c = (AddSourceAuthorityCommand) cmd;
					AddSourceAuthorityInput i = (AddSourceAuthorityInput) input;
					c.setProject(projectRepository.findProjectByName(i.projectName()));
					c.setSystem(i.system());
					c.setExternalId(i.externalId());
					c.setDefersToSystem(i.defersToSystem());
					c.setDefersToExternalId(i.defersToExternalId());
					c.setNote(i.note());
				},
				null,
				cmd -> {
					var edge = ((AddSourceAuthorityCommand) cmd).getEdge();
					return new SourceAuthorityDto(ProvenanceQueryService.toRef(edge.getSubordinate()),
							ProvenanceQueryService.toRef(edge.getSuperior()), edge.getNote());
				});

		registry.register("RemoveSourceAuthority", RemoveSourceAuthorityInput.class,
				factory::newRemoveSourceAuthorityCommand,
				(cmd, input) -> {
					RemoveSourceAuthorityCommand c = (RemoveSourceAuthorityCommand) cmd;
					RemoveSourceAuthorityInput i = (RemoveSourceAuthorityInput) input;
					c.setProject(projectRepository.findProjectByName(i.projectName()));
					c.setSystem(i.system());
					c.setExternalId(i.externalId());
					c.setDefersToSystem(i.defersToSystem());
					c.setDefersToExternalId(i.defersToExternalId());
				},
				null,
				cmd -> Map.of("removed", ((RemoveSourceAuthorityCommand) cmd).isRemoved()));

		registry.register("UpsertFromSource", UpsertFromSourceInput.class,
				factory::newUpsertFromSourceCommand,
				(cmd, input) -> {
					UpsertFromSourceCommand c = (UpsertFromSourceCommand) cmd;
					UpsertFromSourceInput i = (UpsertFromSourceInput) input;
					EditTarget target = editTarget(i.command());
					c.setProject(projectRepository.findProjectByName(i.projectName()));
					c.setEntityType(target.entityType());
					c.setEditCommandFactory(new InnerEdit(i, target));
					c.setSystem(i.system());
					c.setExternalId(i.externalId());
					c.setLocatorType(locatorType(i.locatorType()));
					c.setLocator(i.locator());
					c.setTitle(i.title());
					c.setSourceVersion(i.sourceVersion());
					c.setFragment(i.fragment());
					c.setFragmentText(i.fragmentText());
					c.setEntityId(i.entityId());
				},
				null,
				this::upsertResult);
	}

	private Object upsertResult(Command cmd) {
		UpsertFromSourceCommand c = (UpsertFromSourceCommand) cmd;
		ProjectOrDomainEntity entity = c.getEntity();
		Object entityDto = null;
		if (c.getEditCommand() != null
				&& c.getEditCommandFactory() instanceof InnerEdit inner) {
			entityDto = apiCommandFactory.getObject().extractResult(inner.commandType(),
					c.getEditCommand());
		}
		return new UpsertFromSourceResultDto(c.getStatus().name(),
				c.getEntityType().getSimpleName(),
				entity == null ? null : entity.getId(),
				entity == null ? null : entity.getName(),
				entityDto, c.isSourceChanged(),
				c.getConflictIssue() == null ? null : c.getConflictIssue().getId(),
				c.getCandidates(), ProvenanceQueryService.toSourceDto(c.getSource()));
	}

	/**
	 * Builds the wrapped edit command once the upsert knows the entity: the caller's input with
	 * the project and the resolved id filled in, bound and policy-checked the way the gateway binds
	 * a top-level command.
	 */
	final class InnerEdit implements UpsertFromSourceCommand.EditCommandFactory {

		private final UpsertFromSourceInput outer;
		private final EditTarget target;

		InnerEdit(UpsertFromSourceInput outer, EditTarget target) {
			this.outer = outer;
			this.target = target;
			Object suppliedId = outer.input().get(target.idField());
			if (suppliedId != null) {
				throw new IllegalArgumentException("input must not carry " + target.idField()
						+ ": UpsertFromSource resolves the entity from the source; pass entityId"
						+ " only to choose between entities from the same fragment");
			}
			Object suppliedProject = outer.input().get("projectName");
			if (suppliedProject != null && !outer.projectName().equals(suppliedProject)) {
				throw new IllegalArgumentException("input.projectName must match projectName");
			}
		}

		String commandType() {
			return outer.command();
		}

		@Override
		public Command newEditCommand(Long entityId) {
			Map<String, Object> raw = new HashMap<>(outer.input());
			raw.put("projectName", outer.projectName());
			raw.remove("version");
			if (entityId != null) {
				raw.put(target.idField(), entityId);
			} else {
				raw.remove(target.idField());
			}
			ApiCommandFactory commands = apiCommandFactory.getObject();
			Object bound = objectMapper.convertValue(raw, commands.getInputType(outer.command()));
			CommandPolicy policy = commandPolicy.getIfAvailable();
			if (policy != null) {
				PolicyDecision decision = policy.evaluate(outer.command(), bound);
				if (!decision.allowed()) {
					throw new IllegalArgumentException(decision.reason());
				}
			}
			return commands.newCommand(outer.command(), bound);
		}

		@Override
		public ProjectOrDomainEntity entityOf(Command editCommand) {
			return target.entityOf().apply(editCommand);
		}
	}

	private static EditTarget editTarget(String command) {
		EditTarget target = command == null ? null : EDIT_TARGETS.get(command);
		if (target == null) {
			throw new IllegalArgumentException("command must be one of "
					+ String.join(", ", new java.util.TreeSet<>(EDIT_TARGETS.keySet())));
		}
		return target;
	}

	private ProjectOrDomainEntity loadTarget(Project project, String entityType, Long entityId) {
		Class<? extends ProjectOrDomainEntity> type = ProvenanceEntityTypes.require(entityType);
		return ProvenanceEntityTypes.load(projectRepository, project, type, entityId)
				.orElseThrow(() -> new IllegalArgumentException(entityType + " not found: "
						+ entityId));
	}

	private static SourceLocatorType locatorType(String value) {
		return (value == null || value.isBlank()) ? null : SourceLocatorType.parse(value);
	}
}
