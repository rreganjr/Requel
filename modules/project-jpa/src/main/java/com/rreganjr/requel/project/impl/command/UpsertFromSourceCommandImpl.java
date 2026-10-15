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
package com.rreganjr.requel.project.impl.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.annotation.command.EditPositionCommand;
import com.rreganjr.requel.project.CriterionHash;
import com.rreganjr.requel.project.EntitySourceLink;
import com.rreganjr.requel.project.ExternalSource;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProvenanceStore;
import com.rreganjr.requel.project.ProvenanceStore.RecordedSource;
import com.rreganjr.requel.project.SourceLinkRelation;
import com.rreganjr.requel.project.SourceLinks;
import com.rreganjr.requel.project.SourceLocatorType;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.UpsertFromSourceCommand;
import com.rreganjr.requel.project.impl.ProvenanceEntityTypes;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.validator.EntityValidationException;

/**
 * Issue #272: see {@link UpsertFromSourceCommand}. The decision:
 *
 * <pre>
 * no link for (source, fragment, type)          -> CREATED   (edit command creates)
 * several links, no entityId                    -> AMBIGUOUS (nothing happens)
 * fragment text unchanged                       -> UNCHANGED (link refreshed)
 * fragment changed, entity not edited in Requel -> UPDATED   (edit command updates)
 * fragment changed, entity edited in Requel     -> CONFLICT  (issue raised, link left)
 * </pre>
 *
 * "Edited in Requel" is {@link SourceLinks#isEditedSinceIngest}, which infers it for a link
 * converted from a #71 note (P6). The inferred fingerprint is only filled in when the entity is
 * found unedited, so an edited one keeps conflicting until someone resolves it.
 */
@Controller("upsertFromSourceCommand")
@Scope("prototype")
public class UpsertFromSourceCommandImpl extends AbstractProjectCommand
		implements UpsertFromSourceCommand, AuthorizableCommand {

	/** Longest source wording carried into the conflict position. */
	static final int MAX_POSITION_TEXT = 20_000;

	private Project project;
	private Class<? extends ProjectOrDomainEntity> entityType;
	private EditCommandFactory editCommandFactory;
	private String system;
	private String externalId;
	private SourceLocatorType locatorType;
	private String locator;
	private String title;
	private String sourceVersion;
	private String fragment;
	private String fragmentText;
	private Long entityId;
	private User editedBy;

	private Status status;
	private ExternalSource source;
	private boolean sourceChanged;
	private ProjectOrDomainEntity entity;
	private Command editCommand;
	private EntitySourceLink link;
	private Issue conflictIssue;
	private List<Long> candidates = List.of();

	@Autowired
	public UpsertFromSourceCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setProject(Project project) {
		this.project = project;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public void setEntityType(Class<? extends ProjectOrDomainEntity> entityType) {
		this.entityType = entityType;
	}

	@Override
	public Class<? extends ProjectOrDomainEntity> getEntityType() {
		return entityType;
	}

	@Override
	public void setEditCommandFactory(EditCommandFactory editCommandFactory) {
		this.editCommandFactory = editCommandFactory;
	}

	@Override
	public EditCommandFactory getEditCommandFactory() {
		return editCommandFactory;
	}

	@Override
	public void setSystem(String system) {
		this.system = system;
	}

	@Override
	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	@Override
	public void setLocatorType(SourceLocatorType locatorType) {
		this.locatorType = locatorType;
	}

	@Override
	public void setLocator(String locator) {
		this.locator = locator;
	}

	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public void setSourceVersion(String sourceVersion) {
		this.sourceVersion = sourceVersion;
	}

	@Override
	public void setFragment(String fragment) {
		this.fragment = fragment;
	}

	@Override
	public void setFragmentText(String fragmentText) {
		this.fragmentText = fragmentText;
	}

	@Override
	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	@Override
	public User getEditedBy() {
		return editedBy;
	}

	@Override
	public void setEditedBy(User editedBy) {
		this.editedBy = editedBy;
	}

	@Override
	public Status getStatus() {
		return status;
	}

	@Override
	public ExternalSource getSource() {
		return source;
	}

	@Override
	public boolean isSourceChanged() {
		return sourceChanged;
	}

	@Override
	public ProjectOrDomainEntity getEntity() {
		return entity;
	}

	@Override
	public Command getEditCommand() {
		return editCommand;
	}

	@Override
	public EntitySourceLink getLink() {
		return link;
	}

	@Override
	public Issue getConflictIssue() {
		return conflictIssue;
	}

	@Override
	public List<Long> getCandidates() {
		return candidates;
	}

	/**
	 * The entity type's {@code Edit}: every outcome but AMBIGUOUS writes to the entity or its
	 * annotations. The edit command is authorized again on its own.
	 */
	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		Class<?> type = entityType == null ? ProjectOrDomainEntity.class : entityType;
		return new RequiresStakeholderPermission(ProvenanceEntityTypes.permissionType(type), "Edit");
	}

	@Override
	public void execute() throws Exception {
		validate();
		ProvenanceStore store = RecordSourceCommandImpl.requireProvenanceStore(getProvenanceStore());
		User by = getRepository().get(editedBy);
		String typeName = entityType.getSimpleName();

		ProvenanceStore.SourceSpec spec = new ProvenanceStore.SourceSpec(project.getId(), system,
				externalId, locatorType, locator, title, sourceVersion);
		ProvenanceStore.validateSourceSpec(spec);
		String fragmentHash = CriterionHash.of(fragmentText);

		// Decide before writing anything, so an AMBIGUOUS call leaves the source as it was.
		ExternalSource known = store.findSource(project.getId(), system, externalId).orElse(null);
		List<EntitySourceLink> links = known == null ? List.of()
				: liveLinks(store, store.linksForFragment(known.getId(),
						SourceLinkRelation.DERIVED_FROM, fragment, typeName));
		EntitySourceLink existing;
		if (entityId != null) {
			existing = links.stream().filter(l -> entityId.equals(l.getTargetId())).findFirst()
					.orElseThrow(() -> new IllegalArgumentException(typeName + " " + entityId
							+ " did not come from " + describeFragment()
							+ "; link it with LinkSource first"));
		} else if (links.isEmpty()) {
			existing = null;
		} else if (links.size() > 1) {
			status = Status.AMBIGUOUS;
			source = known;
			candidates = links.stream().map(EntitySourceLink::getTargetId).toList();
			return;
		} else {
			existing = links.get(0);
		}

		// A source's locator and title are the project's (RecordSource needs Project[Edit]); an
		// upsert only needs the entity type's Edit, so it sets them when it first records the
		// source and leaves an existing source's alone. The version it read it at is its to report.
		RecordedSource recorded = store.recordSource(known == null ? spec
				: new ProvenanceStore.SourceSpec(project.getId(), system, externalId, null, null,
						null, sourceVersion), by);
		source = recorded.source();
		sourceChanged = recorded.changed();

		if (existing == null) {
			entity = runEdit(null);
			status = Status.CREATED;
			link = store.link(new ProvenanceStore.LinkSpec(source, SourceLinkRelation.DERIVED_FROM,
					typeName, entity.getId(), fragment, fragmentHash, source.getContentHash(),
					SourceLinks.fingerprint(entity)), by);
			return;
		}

		entity = ProvenanceEntityTypes.load(getProjectRepository(), project, entityType,
				existing.getTargetId()).orElseThrow();
		String current = SourceLinks.fingerprint(entity);
		boolean edited = SourceLinks.isEditedSinceIngest(existing, entity);
		boolean fragmentChanged = existing.getFragmentHash() == null
				|| !existing.getFragmentHash().equals(fragmentHash);

		if (!fragmentChanged) {
			status = Status.UNCHANGED;
			// Refresh which source version this fragment was seen in; fill a missing fingerprint
			// only when the entity is known to be unedited, so an edit is never hidden.
			String fill = (existing.getEntityFingerprint() == null && !edited) ? current : null;
			link = store.link(new ProvenanceStore.LinkSpec(source, SourceLinkRelation.DERIVED_FROM,
					typeName, entity.getId(), fragment, null, source.getContentHash(), fill), by);
		} else if (!edited) {
			entity = runEdit(entity.getId());
			status = Status.UPDATED;
			link = store.link(new ProvenanceStore.LinkSpec(source, SourceLinkRelation.DERIVED_FROM,
					typeName, entity.getId(), fragment, fragmentHash, source.getContentHash(),
					SourceLinks.fingerprint(entity)), by);
		} else {
			status = Status.CONFLICT;
			link = existing;
			conflictIssue = raiseConflict(by, typeName);
		}
	}

	private void validate() {
		if (project == null) {
			throw EntityValidationException.emptyRequiredProperty(Project.class, null, "project",
					EntityExceptionActionType.Updating);
		}
		if (entityType == null) {
			throw new IllegalArgumentException("entityType is required");
		}
		ProvenanceEntityTypes.require(entityType.getSimpleName());
		if (editCommandFactory == null) {
			throw new IllegalStateException("no edit command factory was supplied");
		}
		if (fragmentText == null || fragmentText.isBlank()) {
			throw new IllegalArgumentException(
					"fragmentText is required: it is what a change in the source is detected against");
		}
	}

	/**
	 * Links whose entity no longer exists (a delete path that bypassed the store) are dropped
	 * here and removed, so they cannot make a fragment look ambiguous.
	 */
	private List<EntitySourceLink> liveLinks(ProvenanceStore store, List<EntitySourceLink> links) {
		List<EntitySourceLink> live = new ArrayList<>();
		for (EntitySourceLink candidate : links) {
			Optional<ProjectOrDomainEntity> target = ProvenanceEntityTypes.load(
					getProjectRepository(), project, entityType, candidate.getTargetId());
			if (target.isPresent()) {
				live.add(candidate);
			} else {
				store.unlink(candidate.getSource().getId(), candidate.getRelation(),
						candidate.getTargetType(), candidate.getTargetId(), candidate.getFragment());
			}
		}
		return live;
	}

	private ProjectOrDomainEntity runEdit(Long id) throws Exception {
		Command command = editCommandFactory.newEditCommand(id);
		if (command instanceof EditCommand edit && edit.getEditedBy() == null) {
			edit.setEditedBy(editedBy);
		}
		editCommand = getCommandHandler().execute(command);
		ProjectOrDomainEntity result = editCommandFactory.entityOf(editCommand);
		if (result == null) {
			throw new IllegalStateException("the edit command did not produce an entity");
		}
		return result;
	}

	/**
	 * P9: one issue per link, reused by its text, with the source's new wording added as a
	 * position. Neither carries the locator. #75: raised by the assistant identity, as any other
	 * finding Requel raises on its own, so an X[Edit] holder needs no Annotation[Edit] for it and
	 * the issue reads as Requel's rather than the caller's. Without an assistant user the caller
	 * raises it under their own Annotation[Edit].
	 */
	private Issue raiseConflict(User by, String typeName) throws Exception {
		User author = conflictAuthor(by);
		EditIssueCommand issueCommand = getAnnotationCommandFactory().newEditIssueCommand();
		issueCommand.setGroupingObject(project);
		issueCommand.setAnnotatable(entity);
		issueCommand.setText(conflictText(typeName));
		issueCommand.setMustBeResolved(false);
		issueCommand.setEditedBy(author);
		issueCommand = getCommandHandler().execute(issueCommand);
		Issue issue = issueCommand.getIssue();

		EditPositionCommand positionCommand = getAnnotationCommandFactory().newEditPositionCommand();
		positionCommand.setIssue(issue);
		positionCommand.setText(positionText(fragmentText));
		positionCommand.setEditedBy(author);
		getCommandHandler().execute(positionCommand);
		return issue;
	}

	/** The assistant user, or {@code by} when there is none. */
	private User conflictAuthor(User by) {
		try {
			User assistant = getUserRepository().findUserByUsername(User.ASSISTANT_USERNAME);
			return assistant == null ? by : assistant;
		} catch (com.rreganjr.requel.user.exception.NoSuchUserException e) {
			return by;
		}
	}

	String conflictText(String typeName) {
		return "Source " + describeFragment() + " changed since it was ingested; this "
				+ displayType(typeName) + " was edited in Requel since, so it was not updated.";
	}

	static String positionText(String wording) {
		String text = "Source now reads: " + wording.strip();
		return text.length() <= MAX_POSITION_TEXT ? text : text.substring(0, MAX_POSITION_TEXT);
	}

	private String describeFragment() {
		String normalizedFragment = ProvenanceStore.normalizeFragment(fragment);
		return source == null
				? ProvenanceStore.normalizeSystem(system) + " " + externalId
						+ (normalizedFragment == null ? "" : " " + normalizedFragment)
				: source.getSystem() + " " + source.getExternalId()
						+ (normalizedFragment == null ? "" : " " + normalizedFragment);
	}

	/** "UseCase" -> "use case", "NonUserStakeholder" -> "stakeholder". */
	static String displayType(String typeName) {
		if ("NonUserStakeholder".equals(typeName)) {
			return "stakeholder";
		}
		if ("GlossaryTerm".equals(typeName)) {
			return "glossary term";
		}
		return typeName.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(java.util.Locale.ROOT);
	}
}
