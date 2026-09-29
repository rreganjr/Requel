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
package com.rreganjr.requel.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.impl.ChangeSpellingPosition;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.assistant.core.persistence.AssistantRunEntity;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.impl.GlossaryTermImpl;

/**
 * Issue #268: glossary terms are analyzed. Editing one queues a run for it; spelling (and the
 * vague-word and complexity checks) run on its definition; the glossary-candidate check doesn't,
 * as it would match the term itself; and Fix Spelling corrects the term through
 * {@code EditGlossaryTerm}.
 */
public class GlossaryTermAnalysisIT extends AbstractLexicalAssistantTest {

	@Test
	public void editingATermRaisesASpellingIssueThatFixSpellingCorrects() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("Glossary");
		GlossaryTerm term = newTerm(project, user, "Ledger " + stamp(),
				"The ledger lists every groal.");
		runQueued("GlossaryTerm", term.getId());

		List<LexicalIssue> issues = lexicalIssuesOnTerm(term.getId());
		LexicalIssue spelling = issues.stream()
				.filter(issue -> "groal".equalsIgnoreCase(issue.getWord())
						&& "Text".equals(issue.getAnnotatableEntityPropertyName()))
				.findFirst().orElse(null);
		assertNotNull(spelling, "expected a spelling issue for 'groal' in the definition");
		assertTrue(issues.stream().noneMatch(issue -> issue.getAnnotatableEntityPropertyName() == null),
				"the glossary-candidate check doesn't run on a glossary term");

		Position fix = spelling.getPositions().stream()
				.filter(position -> Hibernate.unproxy(position) instanceof ChangeSpellingPosition)
				.findFirst()
				.orElseThrow(() -> new AssertionError("no Fix Spelling position on the issue"));
		resolve(spelling, fix, null, user);

		GlossaryTermImpl fixed = freshTerm(term.getId());
		assertFalse(fixed.getText().contains("groal"), "Fix Spelling rewrote the definition");
		assertEquals(term.getName(), fixed.getName(), "the name is left as it was");
	}

	// ---- fixtures -------------------------------------------------------------------------

	private GlossaryTerm newTerm(Project project, User user, String name, String text)
			throws Exception {
		EditGlossaryTermCommand command = getProjectCommandFactory().newEditGlossaryTermCommand();
		command.setEditedBy(user);
		command.setProjectOrDomain(project);
		command.setName(name);
		command.setText(text);
		return getCommandHandler().execute(command).getGlossaryTerm();
	}

	private void runQueued(String targetType, Long targetId) {
		AssistantRunEntity queued = assistantRunRepository.findAll().stream()
				.filter(run -> targetType.equals(run.getTargetType())
						&& targetId.equals(run.getTargetId()) && "QUEUED".equals(run.getStatus()))
				.reduce((first, second) -> second)
				.orElseThrow(() -> new AssertionError("no QUEUED run for " + targetType + " "
						+ targetId + ": editing a glossary term should queue one"));
		assistantRunWorker.run(queued.getRunId());
	}

	private GlossaryTermImpl freshTerm(Long termId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			return em.find(GlossaryTermImpl.class, termId);
		} finally {
			em.close();
		}
	}

	private List<LexicalIssue> lexicalIssuesOnTerm(Long termId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			GlossaryTermImpl term = em.find(GlossaryTermImpl.class, termId);
			List<LexicalIssue> issues = term.getAnnotations().stream().map(Hibernate::unproxy)
					.filter(LexicalIssue.class::isInstance).map(LexicalIssue.class::cast)
					.collect(Collectors.toList());
			issues.forEach(issue -> Hibernate.initialize(issue.getPositions()));
			return issues;
		} finally {
			em.close();
		}
	}
}
