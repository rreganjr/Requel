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
package com.rreganjr.requel.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.EditChangeSpellingPositionCommand;
import com.rreganjr.requel.annotation.command.EditLexicalIssueCommand;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.DeleteGlossaryTermCommand;
import com.rreganjr.requel.project.command.DeleteProjectCommand;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.impl.GlossaryTermImpl;

import jakarta.persistence.EntityManager;

/**
 * Issue #348: renaming a glossary term keeps the old name as a synonym of it, so text written
 * against the old name still matches the glossary, whether the rename comes from the term editor
 * or from accepting a vague-word fix on the term's name.
 */
public class GlossaryRenameIT extends AbstractLexicalAssistantTest {

	@Test
	public void aRenameKeepsTheOldNameAsASynonym() throws Exception {
		User user = projectUser();
		Project project = newProject("Rename");
		GlossaryTerm term = newTerm(project, user, "Dry-run event", "A rehearsal before going live.");

		EditGlossaryTermCommand renamed = rename(term, user, "Dry-run webinar");

		assertNotNull(renamed.getCreatedSynonym(), "the rename reports the synonym it created");
		GlossaryTermImpl synonym = termNamed(project, "Dry-run event");
		assertNotNull(synonym, "the old name is still a term");
		assertEquals(term.getId(), synonym.getCanonicalTerm().getId(),
				"its canonical term is the renamed one");
		assertNull(synonym.getText(), "the synonym has no definition");
		assertEquals("Dry-run webinar", termNamed(project, "Dry-run webinar").getName());
	}

	/**
	 * Deleting the kept synonym deletes it. It used to stay behind: the canonical term's
	 * alternates, which cascade PERSIST, still held it, so the delete was undone at flush and the
	 * row (no longer in the project's glossary) then blocked deleting the project.
	 */
	@Test
	public void deletingTheKeptSynonymDeletesIt() throws Exception {
		User user = projectUser();
		Project project = newProject("RenameDeleteSynonym");
		GlossaryTerm term = newTerm(project, user, "Dry-run event", "A rehearsal.");
		rename(term, user, "Dry-run webinar");

		DeleteGlossaryTermCommand delete = getProjectCommandFactory().newDeleteGlossaryTermCommand();
		delete.setEditedBy(user);
		delete.setGlossaryTerm(termNamed(project, "Dry-run event"));
		getCommandHandler().execute(delete);

		List<GlossaryTermImpl> left = termsIn(project);
		assertEquals(1, left.size(), "the synonym row is gone");
		assertEquals("Dry-run webinar", left.get(0).getName());

		DeleteProjectCommand deleteProject = getProjectCommandFactory().newDeleteProjectCommand();
		deleteProject.setEditedBy(user);
		deleteProject.setProject(getProjectRepository().findProjectByName(project.getName()));
		getCommandHandler().execute(deleteProject);
		assertTrue(termsIn(project).isEmpty(), "and the project can be deleted");
	}

	@Test
	public void aCaseOnlyRenameCreatesNoSynonym() throws Exception {
		User user = projectUser();
		Project project = newProject("RenameCase");
		GlossaryTerm term = newTerm(project, user, "dry-run event", "A rehearsal.");

		EditGlossaryTermCommand renamed = rename(term, user, "Dry-Run Event");

		assertNull(renamed.getCreatedSynonym());
		assertEquals(1, termsIn(project).size(), "no second term");
	}

	@Test
	public void anOldNameThatIsAlreadyATermIsNotDuplicated() throws Exception {
		User user = projectUser();
		Project project = newProject("RenameTaken");
		newTerm(project, user, "DRY-RUN EVENT", "Already here, in capitals.");
		GlossaryTerm term = newTerm(project, user, "Dry-run event", "A rehearsal.");

		EditGlossaryTermCommand renamed = rename(term, user, "Dry-run webinar");

		assertNull(renamed.getCreatedSynonym());
		assertEquals(2, termsIn(project).size(), "the existing term covers the old name");
	}

	@Test
	public void renamingASynonymPointsTheNewSynonymAtTheCanonicalTerm() throws Exception {
		User user = projectUser();
		Project project = newProject("RenameChain");
		GlossaryTerm term = newTerm(project, user, "Ledger", "Where entries go.");
		rename(term, user, "Book");
		GlossaryTermImpl synonym = termNamed(project, "Ledger");

		rename(synonym, user, "Journal");

		GlossaryTermImpl again = termNamed(project, "Ledger");
		assertNotNull(again);
		assertEquals(term.getId(), again.getCanonicalTerm().getId(), "no synonym of a synonym");
	}

	/**
	 * #268's path: accepting a "change the word" fix on the term's name (the vague-word check's
	 * position, a ChangeSpelling one) renames the term through EditGlossaryTerm, and the old name
	 * is kept. The issue is built here as the assistant builds it.
	 */
	@Test
	public void acceptingAChangeTheWordFixOnTheNameKeepsTheOldName() throws Exception {
		User user = projectUser();
		Project project = newProject("RenameFix");
		GlossaryTerm term = newTerm(project, user, "Dry-run event", "A rehearsal before going live.");

		EditLexicalIssueCommand issueCommand = getAnnotationCommandFactory().newEditLexicalIssueCommand();
		issueCommand.setEditedBy(user);
		issueCommand.setGroupingObject(project);
		issueCommand.setAnnotatable(term);
		issueCommand.setText("The word \"event\" in the Name is vague and may lead to ambiguity.");
		issueCommand.setMustBeResolved(false);
		issueCommand.setWord("event");
		issueCommand.setAnnotatableEntityPropertyName("Name");
		LexicalIssue issue = (LexicalIssue) getCommandHandler().execute(issueCommand).getIssue();
		EditChangeSpellingPositionCommand positionCommand = getAnnotationCommandFactory()
				.newEditChangeSpellingPositionCommand();
		positionCommand.setEditedBy(user);
		positionCommand.setIssue(issue);
		positionCommand.setText("Change the word \"event\" to \"webinar\".");
		positionCommand.setProposedWord("webinar");
		Position fix = getCommandHandler().execute(positionCommand).getPosition();

		resolve(issue, fix, null, user);

		assertEquals("Dry-run webinar", freshTerm(term.getId()).getName(), "the fix renamed the term");
		GlossaryTermImpl synonym = termNamed(project, "Dry-run event");
		assertNotNull(synonym, "the old name is kept");
		assertEquals(term.getId(), synonym.getCanonicalTerm().getId());
	}

	/** A goal that still says a renamed term's old name raises no glossary candidate for it. */
	@Test
	public void textThatStillSaysTheOldNameStillMatchesTheGlossary() throws Exception {
		ensureDictionaryLoaded();
		User user = projectUser();
		Project project = newProject("RenameGoal");
		GlossaryTerm term = newTerm(project, user, "Dry-run event", "A rehearsal before going live.");
		rename(term, user, "Dry-run webinar");

		Goal goal = newGoal(project, user, "Rehearse", "Schedule the dry-run event before launch.");
		assertNull(glossaryIssue(goal.getId(), "dry-run event"),
				"no glossary candidate for the old name");
		// The glossary check matched the phrase to the synonym and linked the goal to it, as it
		// does for any term the text uses.
		assertTrue(refererIdsOf(termNamed(project, "Dry-run event").getId()).contains(goal.getId()),
				"the old name still matches the glossary");
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

	private EditGlossaryTermCommand rename(GlossaryTerm term, User user, String name)
			throws Exception {
		GlossaryTermImpl current = getProjectRepository().findById(GlossaryTerm.class, term.getId())
				instanceof GlossaryTermImpl impl ? impl : null;
		EditGlossaryTermCommand command = getProjectCommandFactory().newEditGlossaryTermCommand();
		command.setEditedBy(user);
		command.setProjectOrDomain(current.getProjectOrDomain());
		command.setGlossaryTerm(current);
		command.setName(name);
		return getCommandHandler().execute(command);
	}

	private List<GlossaryTermImpl> termsIn(Project project) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			List<GlossaryTermImpl> terms = em.createQuery(
					"select t from GlossaryTermImpl t where t.projectOrDomain.id = :id",
					GlossaryTermImpl.class).setParameter("id", project.getId()).getResultList();
			terms.forEach(t -> {
				if (t.getCanonicalTerm() != null) {
					t.getCanonicalTerm().getId();
				}
			});
			return terms;
		} finally {
			em.close();
		}
	}

	private GlossaryTermImpl termNamed(Project project, String name) {
		return termsIn(project).stream().filter(t -> name.equals(t.getName())).findFirst()
				.orElse(null);
	}

	private java.util.Set<Long> refererIdsOf(Long termId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			return em.find(GlossaryTermImpl.class, termId).getReferers().stream()
					.map(com.rreganjr.requel.project.ProjectOrDomainEntity::getId)
					.collect(java.util.stream.Collectors.toSet());
		} finally {
			em.close();
		}
	}

	private GlossaryTermImpl freshTerm(Long termId) {
		EntityManager em = entityManagerFactory.createEntityManager();
		try {
			return em.find(GlossaryTermImpl.class, termId);
		} finally {
			em.close();
		}
	}
}
