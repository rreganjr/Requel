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

import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ProjectOrDomainEntity;

/**
 * Record that a project entity mentions an existing glossary term, by adding it to that term's
 * referer collection (and the term to the entity's).
 *
 * <p>
 * This exists so the assistants can do that one thing without holding
 * {@code GlossaryTerm[Edit]} (issue #302). The assistants reach it while analyzing text: a
 * phrase that matches a term already in the glossary produces a back-reference, which is
 * bookkeeping about an annotation pass rather than a requirements edit - it changes no name,
 * definition or canonical term, and a stakeholder who may not edit the glossary is not being
 * given a way to edit it. Authorization is therefore {@code Annotation[Edit]}, the same
 * permission the annotations from the same pass need.
 *
 * <p>
 * Everything else about a glossary term still goes through {@code EditGlossaryTermCommand},
 * which requires {@code GlossaryTerm[Edit]} unconditionally - including the user-facing "Add to
 * Glossary" resolver.
 *
 * @author ron
 */
public interface AddGlossaryTermRefererCommand extends EditCommand {

	/**
	 * @param glossaryTerm
	 *            the existing term that gains a referer.
	 */
	public void setGlossaryTerm(GlossaryTerm glossaryTerm);

	/**
	 * @param referer
	 *            the entity whose text mentions the term.
	 */
	public void setReferer(ProjectOrDomainEntity referer);

	/**
	 * @return the term, reloaded, after execution.
	 */
	public GlossaryTerm getGlossaryTerm();
}
