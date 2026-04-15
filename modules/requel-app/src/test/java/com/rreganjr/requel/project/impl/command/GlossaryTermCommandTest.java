/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2025 Ron Regan Jr. All Rights Reserved.
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

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.NoSuchEntityException;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.command.DeleteGlossaryTermCommand;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.ReplaceGlossaryTermCommand;
import com.rreganjr.requel.user.User;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * Integration tests for glossary term management commands:
 * {@link EditGlossaryTermCommand} and {@link DeleteGlossaryTermCommand}.
 *
 * Glossary terms support an alternate/canonical relationship: a term can be
 * marked as an alternate form of another (e.g., "req" is an alternate of
 * "requirement"). DeleteGlossaryTermCommand nulls the canonical pointer on all
 * alternate terms when the canonical is deleted.
 */
public class GlossaryTermCommandTest extends AbstractIntegrationTestCase {

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private Project createProject(String label) throws Exception {
		long ts = System.currentTimeMillis();
		User admin = getUserRepository().findUserByUsername("admin");
		EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
		cmd.setEditedBy(admin);
		cmd.setName(label + "-" + ts);
		cmd.setText("test project for " + label);
		cmd.setOrganizationName("GlossaryTestOrg-" + ts);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getProject();
	}

	private GlossaryTerm createTerm(Project project, String name, String definition) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditGlossaryTermCommand cmd = getProjectCommandFactory().newEditGlossaryTermCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName(name);
		cmd.setText(definition);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getGlossaryTerm();
	}

	// -------------------------------------------------------------------------
	// EditGlossaryTermCommand
	// -------------------------------------------------------------------------

	@Test
	public void createGlossaryTerm() throws Exception {
		Project project = createProject("Glossary-create");
		User admin = getUserRepository().findUserByUsername("admin");

		EditGlossaryTermCommand cmd = getProjectCommandFactory().newEditGlossaryTermCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName("stakeholder");
		cmd.setText("A person or organization that has an interest in the system being developed.");
		cmd = getCommandHandler().execute(cmd);

		GlossaryTerm term = cmd.getGlossaryTerm();
		assertNotNull(term, "glossary term should have been created");
		assertEquals("stakeholder", term.getName(), "term name should match");
		assertEquals("A person or organization that has an interest in the system being developed.",
				term.getText(), "term definition should match");
		assertDoesNotThrow(
				() -> getProjectRepository().findGlossaryTermForProjectOrDomain(project, "stakeholder"),
				"newly created term must be findable on the project");
	}

	@Test
	public void editGlossaryTerm() throws Exception {
		Project project = createProject("Glossary-edit");
		User admin = getUserRepository().findUserByUsername("admin");
		GlossaryTerm original = createTerm(project, "requirement",
				"A condition or capability needed by a user.");

		EditGlossaryTermCommand editCmd = getProjectCommandFactory().newEditGlossaryTermCommand();
		editCmd.setEditedBy(admin);
		editCmd.setProjectOrDomain(project);
		editCmd.setGlossaryTerm(original);
		editCmd.setName("requirement");
		editCmd.setText("A condition or capability that a system must satisfy to meet stakeholder needs.");
		editCmd = getCommandHandler().execute(editCmd);

		GlossaryTerm updated = editCmd.getGlossaryTerm();
		assertEquals("requirement", updated.getName(), "name should be unchanged");
		assertEquals("A condition or capability that a system must satisfy to meet stakeholder needs.",
				updated.getText(), "definition should have been updated");
	}

	@Test
	public void createAlternateTerm() throws Exception {
		Project project = createProject("Glossary-alternate");
		User admin = getUserRepository().findUserByUsername("admin");
		GlossaryTerm canonical = createTerm(project, "requirement", "A condition or capability.");

		EditGlossaryTermCommand cmd = getProjectCommandFactory().newEditGlossaryTermCommand();
		cmd.setEditedBy(admin);
		cmd.setProjectOrDomain(project);
		cmd.setName("req");
		cmd.setText("Abbreviation for requirement.");
		cmd.setCanonicalTerm(canonical);
		cmd = getCommandHandler().execute(cmd);

		GlossaryTerm alternate = cmd.getGlossaryTerm();
		assertNotNull(alternate.getCanonicalTerm(), "alternate term should reference its canonical term");
		assertEquals(canonical.getName(), alternate.getCanonicalTerm().getName(),
				"canonical term should be 'requirement'");
		// The canonical term's alternate set should include the new term
		GlossaryTerm reloadedCanonical = getProjectRepository()
				.findGlossaryTermForProjectOrDomain(project, "requirement");
		assertTrue(reloadedCanonical.getAlternateTerms().stream()
				.anyMatch(t -> "req".equals(t.getName())),
				"canonical term's alternate set should include 'req'");
	}

	@Test
	public void duplicateGlossaryTermIsRejected() throws Exception {
		Project project = createProject("Glossary-dup");
		User admin = getUserRepository().findUserByUsername("admin");
		createTerm(project, "actor", "A user or external system that interacts with the system.");

		assertThrows(EntityException.class, () -> {
			EditGlossaryTermCommand dup = getProjectCommandFactory().newEditGlossaryTermCommand();
			dup.setEditedBy(admin);
			dup.setProjectOrDomain(project);
			dup.setName("actor");
			dup.setText("Duplicate definition.");
			getCommandHandler().execute(dup);
		}, "duplicate glossary term name on the same project should be rejected");
	}

	// -------------------------------------------------------------------------
	// DeleteGlossaryTermCommand
	// -------------------------------------------------------------------------

	@Test
	public void deleteGlossaryTerm() throws Exception {
		Project project = createProject("Glossary-delete");
		User admin = getUserRepository().findUserByUsername("admin");
		GlossaryTerm term = createTerm(project, "ToDelete", "This term will be deleted.");

		DeleteGlossaryTermCommand deleteCmd = getProjectCommandFactory().newDeleteGlossaryTermCommand();
		deleteCmd.setEditedBy(admin);
		deleteCmd.setGlossaryTerm(term);
		getCommandHandler().execute(deleteCmd);

		assertThrows(NoSuchEntityException.class,
				() -> getProjectRepository().findGlossaryTermForProjectOrDomain(project, "ToDelete"),
				"deleted glossary term should no longer be findable");
	}

	// -------------------------------------------------------------------------
	// ReplaceGlossaryTermCommand
	// -------------------------------------------------------------------------

	@Test
	public void replaceGlossaryTermRewritesTextInReferringGoal() throws Exception {
		Project project = createProject("Glossary-replace");
		User admin = getUserRepository().findUserByUsername("admin");

		// Create the canonical term
		GlossaryTerm canonical = createTerm(project, "Bug",
				"A defect in the software that causes it to behave incorrectly.");

		// Create a goal whose name and text contain the alias term's name
		EditGoalCommand goalCmd = getProjectCommandFactory().newEditGoalCommand();
		goalCmd.setEditedBy(admin);
		goalCmd.setGoalContainer(project);
		goalCmd.setName("Fix the Defect");
		goalCmd.setText("We need to fix the Defect in the login flow before release.");
		goalCmd = getCommandHandler().execute(goalCmd);
		Goal goal = goalCmd.getGoal();

		// Create the alias term with the goal as a referring entity
		EditGlossaryTermCommand aliasCmd = getProjectCommandFactory().newEditGlossaryTermCommand();
		aliasCmd.setEditedBy(admin);
		aliasCmd.setProjectOrDomain(project);
		aliasCmd.setName("Defect");
		aliasCmd.setText("Alternate term for Bug.");
		aliasCmd.setCanonicalTerm(canonical);
		aliasCmd.setAddReferers(Set.<ProjectOrDomainEntity>of(goal));
		aliasCmd = getCommandHandler().execute(aliasCmd);
		GlossaryTerm alias = aliasCmd.getGlossaryTerm();

		// Replace the alias term with the canonical across all referring entities
		ReplaceGlossaryTermCommand replaceCmd = getProjectCommandFactory().newReplaceGlossaryTermCommand();
		replaceCmd.setEditedBy(admin);
		replaceCmd.setGlossaryTerm(alias);
		getCommandHandler().execute(replaceCmd);

		// Verify the goal text now references the canonical term name
		Goal reloaded = getProjectRepository().findGoalByProjectOrDomainAndName(project, "Fix the Bug");
		assertNotNull(reloaded, "goal name should have been updated to canonical term name");
		assertFalse(reloaded.getText().contains("Defect"),
				"alias term name should have been replaced in goal text");
		assertTrue(reloaded.getText().contains("Bug"),
				"canonical term name should appear in goal text after replacement");
	}

	@Test
	public void deletingCanonicalTermClearsAlternates() throws Exception {
		Project project = createProject("Glossary-delete-canonical");
		User admin = getUserRepository().findUserByUsername("admin");
		GlossaryTerm canonical = createTerm(project, "requirement", "A condition or capability.");

		// Create an alternate that points to the canonical
		EditGlossaryTermCommand altCmd = getProjectCommandFactory().newEditGlossaryTermCommand();
		altCmd.setEditedBy(admin);
		altCmd.setProjectOrDomain(project);
		altCmd.setName("req");
		altCmd.setText("Short for requirement.");
		altCmd.setCanonicalTerm(canonical);
		altCmd = getCommandHandler().execute(altCmd);
		GlossaryTerm alternate = altCmd.getGlossaryTerm();
		assertNotNull(alternate.getCanonicalTerm(), "pre-condition: alternate should point to canonical");

		// Delete the canonical term
		DeleteGlossaryTermCommand deleteCmd = getProjectCommandFactory().newDeleteGlossaryTermCommand();
		deleteCmd.setEditedBy(admin);
		deleteCmd.setGlossaryTerm(canonical);
		getCommandHandler().execute(deleteCmd);

		// The alternate term should still exist but with no canonical pointer
		GlossaryTerm reloadedAlternate = getProjectRepository()
				.findGlossaryTermForProjectOrDomain(project, "req");
		assertNull(reloadedAlternate.getCanonicalTerm(),
				"alternate term's canonical pointer should be cleared after the canonical is deleted");
	}
}
