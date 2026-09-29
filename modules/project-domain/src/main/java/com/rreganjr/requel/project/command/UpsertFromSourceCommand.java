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
package com.rreganjr.requel.project.command;

import java.util.List;

import com.rreganjr.command.Command;
import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.project.EntitySourceLink;
import com.rreganjr.requel.project.ExternalSource;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.SourceLocatorType;

/**
 * Issue #272: create or update an entity from a fragment of an external source, enforcing the
 * rule that Requel is authoritative. In one transaction it records the source, resolves the
 * fragment's existing links for the entity type, and decides:
 * <ul>
 * <li>{@link Status#CREATED} — no entity came from this fragment yet: the edit command creates one;</li>
 * <li>{@link Status#UNCHANGED} — the fragment's text is what was ingested: nothing is edited;</li>
 * <li>{@link Status#UPDATED} — the fragment changed and the entity was not edited in Requel since
 * the last ingest: the edit command updates it;</li>
 * <li>{@link Status#CONFLICT} — the fragment changed and the entity was edited in Requel since:
 * the entity is left alone and an issue carrying the source's new wording is raised on it;</li>
 * <li>{@link Status#AMBIGUOUS} — several entities of the type came from the fragment and no
 * {@code entityId} said which: nothing happens, and {@link #getCandidates()} lists them.</li>
 * </ul>
 * The edit command is built by the caller's {@link EditCommandFactory} with the resolved entity id
 * (null to create) and runs with its own authorization. The composite itself requires the entity
 * type's {@code Edit} permission, which also covers the conflict issue.
 */
public interface UpsertFromSourceCommand extends EditCommand, ProjectScopedCommand {

	/** The outcome of an upsert. */
	enum Status {
		CREATED, UNCHANGED, UPDATED, CONFLICT, AMBIGUOUS
	}

	/** Builds the edit command for the entity, once the upsert knows which one it is. */
	interface EditCommandFactory {

		/**
		 * @param entityId the entity to update, or null to create one
		 * @return the bound edit command, not yet executed
		 */
		Command newEditCommand(Long entityId) throws Exception;

		/** @return the entity the executed {@code editCommand} created or updated. */
		ProjectOrDomainEntity entityOf(Command editCommand);
	}

	void setProject(Project project);

	/** The entity type, e.g. {@code Goal.class}: the interface the edit command creates. */
	void setEntityType(Class<? extends ProjectOrDomainEntity> entityType);

	Class<? extends ProjectOrDomainEntity> getEntityType();

	void setEditCommandFactory(EditCommandFactory editCommandFactory);

	EditCommandFactory getEditCommandFactory();

	void setSystem(String system);

	void setExternalId(String externalId);

	void setLocatorType(SourceLocatorType locatorType);

	void setLocator(String locator);

	void setTitle(String title);

	/** The source's current content hash (for a file, the SHA-256 of its bytes); may be null. */
	void setSourceVersion(String sourceVersion);

	/** A stable key for the fragment (not a position that renumbers); null = the whole source. */
	void setFragment(String fragment);

	/** The fragment's current text. Required: it is what change is detected against. */
	void setFragmentText(String fragmentText);

	/** Which entity to update when several came from the fragment; normally null. */
	void setEntityId(Long entityId);

	Status getStatus();

	ExternalSource getSource();

	/** True when the source's content hash differs from the one recorded before this call. */
	boolean isSourceChanged();

	/** The entity created, updated, left unchanged or in conflict; null when ambiguous. */
	ProjectOrDomainEntity getEntity();

	/** The executed edit command (CREATED/UPDATED), for the caller's result extraction. */
	Command getEditCommand();

	EntitySourceLink getLink();

	/** The conflict issue (CONFLICT only). */
	Issue getConflictIssue();

	/** The ids of the entities that came from the fragment (AMBIGUOUS only). */
	List<Long> getCandidates();
}
