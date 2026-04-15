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
package com.rreganjr.requel.annotation.impl.command;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.annotation.Argument;
import com.rreganjr.requel.annotation.ArgumentPositionSupportLevel;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Note;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.DeleteArgumentCommand;
import com.rreganjr.requel.annotation.command.DeleteIssueCommand;
import com.rreganjr.requel.annotation.command.DeleteNoteCommand;
import com.rreganjr.requel.annotation.command.DeletePositionCommand;
import com.rreganjr.requel.annotation.command.EditArgumentCommand;
import com.rreganjr.requel.annotation.command.EditChangeSpellingPositionCommand;
import com.rreganjr.requel.annotation.command.EditAddWordToDictionaryPositionCommand;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.command.EditAddActorToProjectPositionCommand;
import com.rreganjr.requel.project.command.EditAddWordToGlossaryPositionCommand;
import com.rreganjr.requel.annotation.command.EditLexicalIssueCommand;
import com.rreganjr.requel.annotation.command.EditNoteCommand;
import com.rreganjr.requel.annotation.command.EditPositionCommand;
import com.rreganjr.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import com.rreganjr.requel.annotation.command.ResolveIssueCommand;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.user.User;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the IBIS annotation command layer:
 * {@link EditIssueCommand}, {@link EditPositionCommand}, {@link EditArgumentCommand},
 * {@link DeleteIssueCommand}, {@link DeletePositionCommand}, {@link DeleteArgumentCommand},
 * {@link EditNoteCommand}, {@link DeleteNoteCommand}, {@link ResolveIssueCommand},
 * {@link RemoveAnnotationFromAnnotatableCommand}, and the two lexical-resolve variants.
 *
 * Annotations follow an IBIS hierarchy: Issue → Position → Argument.
 * Issues are attached to an annotatable entity (e.g. a Goal) and grouped by a
 * project (the groupingObject). Positions are proposed solutions to an issue.
 * Arguments express support or opposition to a position.
 *
 * DeleteIssue cascades to delete all positions (and their arguments).
 * DeletePosition cascades to delete all arguments on that position.
 * DeleteArgument removes only the single argument.
 *
 * RemoveAnnotationFromAnnotatable detaches a single annotation from one annotatable;
 * if no annotatables remain the annotation is auto-deleted.
 *
 * ResolveIssue marks an issue resolved via a chosen position.
 * ResolveIssueWithChangeSpellingPosition additionally rewrites the misspelled word
 * in the annotatable entity's text field.
 * ResolveIssueWithAddWordToDictionaryPosition additionally adds the word to the
 * dictionary before resolving.
 */
public class AnnotationCommandTest extends AbstractIntegrationTestCase {

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
        cmd.setOrganizationName("AnnotationTestOrg-" + ts);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getProject();
    }

    private Goal createGoal(Project project, String name) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
        cmd.setEditedBy(admin);
        cmd.setGoalContainer(project);
        cmd.setName(name);
        cmd.setText("A goal for annotation tests.");
        cmd = getCommandHandler().execute(cmd);
        return cmd.getGoal();
    }

    private Issue createIssue(Project project, Goal goal, String text) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditIssueCommand cmd = getAnnotationCommandFactory().newEditIssueCommand();
        cmd.setEditedBy(admin);
        cmd.setGroupingObject(project);
        cmd.setAnnotatable(goal);
        cmd.setText(text);
        cmd.setMustBeResolved(false);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getIssue();
    }

    private Position createPosition(Issue issue, String text) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditPositionCommand cmd = getAnnotationCommandFactory().newEditPositionCommand();
        cmd.setEditedBy(admin);
        cmd.setIssue(issue);
        cmd.setText(text);
        cmd = getCommandHandler().execute(cmd);
        return cmd.getPosition();
    }

    private Argument createArgument(Position position, String text,
            ArgumentPositionSupportLevel level) throws Exception {
        User admin = getUserRepository().findUserByUsername("admin");
        EditArgumentCommand cmd = getAnnotationCommandFactory().newEditArgumentCommand();
        cmd.setEditedBy(admin);
        cmd.setPosition(position);
        cmd.setText(text);
        cmd.setSupportLevelName(level.name());
        cmd = getCommandHandler().execute(cmd);
        return cmd.getArgument();
    }

    // -------------------------------------------------------------------------
    // EditIssueCommand
    // -------------------------------------------------------------------------

    @Test
    public void createIssue() throws Exception {
        Project project = createProject("Annotation-issue-create");
        Goal goal = createGoal(project, "Support multi-user access");

        Issue issue = createIssue(project, goal,
                "How many concurrent users must be supported?");

        assertNotNull(issue, "issue should have been created");
        assertEquals("How many concurrent users must be supported?", issue.getText(),
                "issue text should match");
        assertFalse(issue.isMustBeResolved(), "mustBeResolved should be false");

        // Reload goal so Hibernate initializes its annotations collection in a fresh session
        Goal reloaded = getProjectRepository()
                .findGoalByProjectOrDomainAndName(project, "Support multi-user access");
        assertTrue(reloaded.getAnnotations().stream().anyMatch(a -> a.equals(issue)),
                "issue should appear in goal annotations");
    }

    @Test
    public void editIssue() throws Exception {
        Project project = createProject("Annotation-issue-edit");
        Goal goal = createGoal(project, "Ensure data integrity");
        Issue issue = createIssue(project, goal, "Original issue text");

        User admin = getUserRepository().findUserByUsername("admin");
        EditIssueCommand cmd = getAnnotationCommandFactory().newEditIssueCommand();
        cmd.setEditedBy(admin);
        cmd.setGroupingObject(project);
        cmd.setAnnotatable(goal);
        cmd.setIssue(issue);
        cmd.setText("Updated issue text after review");
        cmd.setMustBeResolved(true);
        cmd = getCommandHandler().execute(cmd);

        Issue updated = cmd.getIssue();
        assertEquals("Updated issue text after review", updated.getText(),
                "issue text should reflect the update");
        assertTrue(updated.isMustBeResolved(), "mustBeResolved should be true after update");
    }

    // -------------------------------------------------------------------------
    // EditPositionCommand
    // -------------------------------------------------------------------------

    @Test
    public void createPosition() throws Exception {
        Project project = createProject("Annotation-position-create");
        Goal goal = createGoal(project, "Define session limits");
        Issue issue = createIssue(project, goal, "What is the maximum session length?");

        Position position = createPosition(issue, "Limit sessions to 30 minutes of inactivity");

        assertNotNull(position, "position should have been created");
        assertEquals("Limit sessions to 30 minutes of inactivity", position.getText(),
                "position text should match");

        // Reload issue to get a fresh session with initialized positions collection
        Issue reloaded = getAnnotationRepository().findIssue(project, goal,
                "What is the maximum session length?");
        assertTrue(reloaded.getPositions().stream().anyMatch(p -> p.equals(position)),
                "position should appear in issue positions");
    }

    @Test
    public void editPosition() throws Exception {
        Project project = createProject("Annotation-position-edit");
        Goal goal = createGoal(project, "Control access times");
        Issue issue = createIssue(project, goal, "Should sessions expire automatically?");
        Position position = createPosition(issue, "Original position text");

        User admin = getUserRepository().findUserByUsername("admin");
        EditPositionCommand cmd = getAnnotationCommandFactory().newEditPositionCommand();
        cmd.setEditedBy(admin);
        cmd.setIssue(issue);
        cmd.setPosition(position);
        cmd.setText("Revised: sessions expire after 60 minutes of inactivity");
        cmd = getCommandHandler().execute(cmd);

        Position updated = cmd.getPosition();
        assertEquals("Revised: sessions expire after 60 minutes of inactivity", updated.getText(),
                "position text should reflect the update");
    }

    // -------------------------------------------------------------------------
    // EditArgumentCommand
    // -------------------------------------------------------------------------

    @Test
    public void createArgument() throws Exception {
        Project project = createProject("Annotation-argument-create");
        Goal goal = createGoal(project, "Minimize security risk");
        Issue issue = createIssue(project, goal, "How should passwords be stored?");
        Position position = createPosition(issue, "Use bcrypt with a cost factor of 12");

        Argument argument = createArgument(position,
                "bcrypt is a well-established algorithm proven resistant to brute force",
                ArgumentPositionSupportLevel.For);

        assertNotNull(argument, "argument should have been created");
        assertEquals("bcrypt is a well-established algorithm proven resistant to brute force",
                argument.getText(), "argument text should match");
        assertEquals(ArgumentPositionSupportLevel.For, argument.getSupportLevel(),
                "argument support level should be For");

        // Reload position to get a fresh session with initialized arguments collection
        Position reloaded = getAnnotationRepository().findPosition(project,
                "Use bcrypt with a cost factor of 12");
        assertTrue(reloaded.getArguments().stream().anyMatch(a -> a.equals(argument)),
                "argument should appear in position arguments");
    }

    @Test
    public void editArgument() throws Exception {
        Project project = createProject("Annotation-argument-edit");
        Goal goal = createGoal(project, "Maintain audit history");
        Issue issue = createIssue(project, goal, "How long should audit logs be retained?");
        Position position = createPosition(issue, "Retain logs for 90 days");
        Argument argument = createArgument(position, "Original argument text",
                ArgumentPositionSupportLevel.Neutral);

        User admin = getUserRepository().findUserByUsername("admin");
        EditArgumentCommand cmd = getAnnotationCommandFactory().newEditArgumentCommand();
        cmd.setEditedBy(admin);
        cmd.setPosition(position);
        cmd.setArgument(argument);
        cmd.setText("Compliance requires a minimum of 90 days retention");
        cmd.setSupportLevelName(ArgumentPositionSupportLevel.StronglyFor.name());
        cmd = getCommandHandler().execute(cmd);

        Argument updated = cmd.getArgument();
        assertEquals("Compliance requires a minimum of 90 days retention", updated.getText(),
                "argument text should reflect the update");
        assertEquals(ArgumentPositionSupportLevel.StronglyFor, updated.getSupportLevel(),
                "argument support level should be updated to StronglyFor");
    }

    // -------------------------------------------------------------------------
    // DeleteArgumentCommand
    // -------------------------------------------------------------------------

    @Test
    public void deleteArgument() throws Exception {
        Project project = createProject("Annotation-argument-delete");
        Goal goal = createGoal(project, "Protect user data");
        Issue issue = createIssue(project, goal, "Should we encrypt data at rest?");
        Position position = createPosition(issue, "Encrypt using AES-256");
        Argument argument = createArgument(position,
                "AES-256 meets all current regulatory requirements",
                ArgumentPositionSupportLevel.For);

        // Reload to confirm argument is present before deleting
        Position before = getAnnotationRepository().findPosition(project, "Encrypt using AES-256");
        assertFalse(before.getArguments().isEmpty(), "argument should be present before delete");

        User admin = getUserRepository().findUserByUsername("admin");
        DeleteArgumentCommand cmd = getAnnotationCommandFactory().newDeleteArgumentCommand();
        cmd.setEditedBy(admin);
        cmd.setArgument(argument);
        getCommandHandler().execute(cmd);

        // Reload position to verify argument was removed from DB
        Position reloaded = getAnnotationRepository().findPosition(project, "Encrypt using AES-256");
        assertTrue(reloaded.getArguments().isEmpty(),
                "argument should be absent from position after delete");
    }

    // -------------------------------------------------------------------------
    // DeletePositionCommand
    // -------------------------------------------------------------------------

    @Test
    public void deletePosition() throws Exception {
        Project project = createProject("Annotation-position-delete");
        Goal goal = createGoal(project, "Ensure availability");
        Issue issue = createIssue(project, goal, "What is the target uptime?");
        Position position = createPosition(issue, "Target 99.9% uptime");
        createArgument(position, "This is achievable with redundant infrastructure",
                ArgumentPositionSupportLevel.For);

        // Reload to confirm position is present before deleting
        Issue before = getAnnotationRepository().findIssue(project, goal,
                "What is the target uptime?");
        assertFalse(before.getPositions().isEmpty(), "position should be present before delete");

        User admin = getUserRepository().findUserByUsername("admin");
        DeletePositionCommand cmd = getAnnotationCommandFactory().newDeletePositionCommand();
        cmd.setEditedBy(admin);
        cmd.setPosition(position);
        getCommandHandler().execute(cmd);

        // Reload issue and verify position (and its arguments) were removed
        Issue reloaded = getAnnotationRepository().findIssue(project, goal,
                "What is the target uptime?");
        assertTrue(reloaded.getPositions().isEmpty(),
                "position should be absent from issue after delete");
    }

    // -------------------------------------------------------------------------
    // DeleteIssueCommand
    // -------------------------------------------------------------------------

    @Test
    public void deleteIssue() throws Exception {
        Project project = createProject("Annotation-issue-delete");
        Goal goal = createGoal(project, "Meet performance targets");
        Issue issue = createIssue(project, goal, "What response time should we target?");
        Position position = createPosition(issue, "Response time under 200ms for 95% of requests");
        createArgument(position, "Industry standard for web applications",
                ArgumentPositionSupportLevel.For);

        // Reload goal to confirm issue is present before deleting
        Goal before = getProjectRepository()
                .findGoalByProjectOrDomainAndName(project, "Meet performance targets");
        assertTrue(before.getAnnotations().stream().anyMatch(a -> a.equals(issue)),
                "issue should be present before delete");

        User admin = getUserRepository().findUserByUsername("admin");
        DeleteIssueCommand cmd = getAnnotationCommandFactory().newDeleteIssueCommand();
        cmd.setEditedBy(admin);
        cmd.setIssue(issue);
        getCommandHandler().execute(cmd);

        // Reload goal and verify issue is gone
        Goal reloaded = getProjectRepository()
                .findGoalByProjectOrDomainAndName(project, "Meet performance targets");
        assertTrue(reloaded.getAnnotations().isEmpty(),
                "issue should be absent from goal annotations after delete");
    }

    // -------------------------------------------------------------------------
    // EditNoteCommand / DeleteNoteCommand
    // -------------------------------------------------------------------------

    @Test
    public void createNote() throws Exception {
        Project project = createProject("Annotation-note-create");
        Goal goal = createGoal(project, "Capture stakeholder feedback");
        User admin = getUserRepository().findUserByUsername("admin");

        EditNoteCommand cmd = getAnnotationCommandFactory().newEditNoteCommand();
        cmd.setEditedBy(admin);
        cmd.setGroupingObject(project);
        cmd.setAnnotatable(goal);
        cmd.setText("Review this goal with the customer before finalizing.");
        cmd = getCommandHandler().execute(cmd);

        Note note = cmd.getNote();
        assertNotNull(note, "note should have been created");
        assertEquals("Review this goal with the customer before finalizing.", note.getText(),
                "note text should match");

        Goal reloaded = getProjectRepository()
                .findGoalByProjectOrDomainAndName(project, "Capture stakeholder feedback");
        assertTrue(reloaded.getAnnotations().stream().anyMatch(a -> a.equals(note)),
                "note should appear in goal annotations");
    }

    @Test
    public void editNote() throws Exception {
        Project project = createProject("Annotation-note-edit");
        Goal goal = createGoal(project, "Prioritize accessibility");
        User admin = getUserRepository().findUserByUsername("admin");

        EditNoteCommand createCmd = getAnnotationCommandFactory().newEditNoteCommand();
        createCmd.setEditedBy(admin);
        createCmd.setGroupingObject(project);
        createCmd.setAnnotatable(goal);
        createCmd.setText("Original note text");
        createCmd = getCommandHandler().execute(createCmd);
        Note note = createCmd.getNote();

        EditNoteCommand editCmd = getAnnotationCommandFactory().newEditNoteCommand();
        editCmd.setEditedBy(admin);
        editCmd.setGroupingObject(project);
        editCmd.setAnnotatable(goal);
        editCmd.setNote(note);
        editCmd.setText("Updated: confirmed with stakeholders — accessibility is P0");
        editCmd = getCommandHandler().execute(editCmd);

        assertEquals("Updated: confirmed with stakeholders — accessibility is P0",
                editCmd.getNote().getText(), "note text should be updated");
    }

    @Test
    public void deleteNote() throws Exception {
        Project project = createProject("Annotation-note-delete");
        Goal goal = createGoal(project, "Track open questions");
        User admin = getUserRepository().findUserByUsername("admin");

        EditNoteCommand createCmd = getAnnotationCommandFactory().newEditNoteCommand();
        createCmd.setEditedBy(admin);
        createCmd.setGroupingObject(project);
        createCmd.setAnnotatable(goal);
        createCmd.setText("Note to be deleted");
        createCmd = getCommandHandler().execute(createCmd);
        Note note = createCmd.getNote();

        DeleteNoteCommand deleteCmd = getAnnotationCommandFactory().newDeleteNoteCommand();
        deleteCmd.setEditedBy(admin);
        deleteCmd.setNote(note);
        getCommandHandler().execute(deleteCmd);

        Goal reloaded = getProjectRepository()
                .findGoalByProjectOrDomainAndName(project, "Track open questions");
        assertTrue(reloaded.getAnnotations().isEmpty(),
                "note should be absent from goal annotations after delete");
    }

    // -------------------------------------------------------------------------
    // ResolveIssueCommand
    // -------------------------------------------------------------------------

    @Test
    public void resolveIssueWithPosition() throws Exception {
        Project project = createProject("Annotation-resolve-base");
        Goal goal = createGoal(project, "Define SLA requirements");
        Issue issue = createIssue(project, goal, "What is the acceptable response time?");
        Position position = createPosition(issue, "Response time must be under 500ms for all reads");
        User admin = getUserRepository().findUserByUsername("admin");

        ResolveIssueCommand cmd = getAnnotationCommandFactory().newResolveIssueCommand(position);
        cmd.setEditedBy(admin);
        cmd.setIssue(issue);
        cmd.setPosition(position);
        cmd.setAnnotatable(goal);
        getCommandHandler().execute(cmd);

        Issue resolved = getAnnotationRepository().findIssue(project, goal,
                "What is the acceptable response time?");
        assertTrue(resolved.isResolved(), "issue should be marked resolved");
        assertNotNull(resolved.getResolvedByPosition(),
                "issue should reference the resolving position");
        assertEquals(position.getText(), resolved.getResolvedByPosition().getText(),
                "resolvedByPosition should be the chosen position");
    }

    // -------------------------------------------------------------------------
    // RemoveAnnotationFromAnnotatableCommand
    // -------------------------------------------------------------------------

    @Test
    public void removeAnnotationFromAnnotatableKeepsAnnotationWhenShared() throws Exception {
        Project project = createProject("Annotation-remove-shared");
        Goal goal1 = createGoal(project, "Shared goal A");
        Goal goal2 = createGoal(project, "Shared goal B");
        User admin = getUserRepository().findUserByUsername("admin");

        // Create an issue attached to both goals
        EditIssueCommand issueCmd = getAnnotationCommandFactory().newEditIssueCommand();
        issueCmd.setEditedBy(admin);
        issueCmd.setGroupingObject(project);
        issueCmd.setAnnotatable(goal1);
        issueCmd.setText("Cross-cutting concern shared by both goals");
        issueCmd.setMustBeResolved(false);
        issueCmd = getCommandHandler().execute(issueCmd);
        Issue issue = issueCmd.getIssue();

        // Also attach the same issue to goal2
        issueCmd = getAnnotationCommandFactory().newEditIssueCommand();
        issueCmd.setEditedBy(admin);
        issueCmd.setGroupingObject(project);
        issueCmd.setAnnotatable(goal2);
        issueCmd.setIssue(issue);
        issueCmd.setText(issue.getText());
        issueCmd.setMustBeResolved(false);
        issueCmd = getCommandHandler().execute(issueCmd);

        // Remove the issue from goal1 only — it should remain on goal2
        RemoveAnnotationFromAnnotatableCommand removeCmd =
                getAnnotationCommandFactory().newRemoveAnnotationFromAnnotatableCommand();
        removeCmd.setEditedBy(admin);
        removeCmd.setAnnotatable(goal1);
        removeCmd.setAnnotation(issue);
        getCommandHandler().execute(removeCmd);

        Goal reloadedGoal1 = getProjectRepository()
                .findGoalByProjectOrDomainAndName(project, "Shared goal A");
        assertTrue(reloadedGoal1.getAnnotations().isEmpty(),
                "issue should be removed from goal1");

        Goal reloadedGoal2 = getProjectRepository()
                .findGoalByProjectOrDomainAndName(project, "Shared goal B");
        assertFalse(reloadedGoal2.getAnnotations().isEmpty(),
                "issue should still be present on goal2");
    }

    @Test
    public void removeAnnotationFromAnnotatableDeletesIssueWhenLastAnnotatable() throws Exception {
        Project project = createProject("Annotation-remove-last");
        Goal goal = createGoal(project, "Sole annotatable goal");
        Issue issue = createIssue(project, goal, "Will this issue be deleted?");
        User admin = getUserRepository().findUserByUsername("admin");

        RemoveAnnotationFromAnnotatableCommand removeCmd =
                getAnnotationCommandFactory().newRemoveAnnotationFromAnnotatableCommand();
        removeCmd.setEditedBy(admin);
        removeCmd.setAnnotatable(goal);
        removeCmd.setAnnotation(issue);
        getCommandHandler().execute(removeCmd);

        // Goal should have no annotations now
        Goal reloaded = getProjectRepository()
                .findGoalByProjectOrDomainAndName(project, "Sole annotatable goal");
        assertTrue(reloaded.getAnnotations().isEmpty(),
                "issue should be removed from the goal's annotations");
    }

    // -------------------------------------------------------------------------
    // ResolveIssueWithChangeSpellingPositionCommand
    // -------------------------------------------------------------------------

    @Test
    public void resolveIssueWithChangeSpellingFixesTextInAnnotatable() throws Exception {
        Project project = createProject("Annotation-spelling");
        User admin = getUserRepository().findUserByUsername("admin");

        // Create a goal whose text contains a misspelled word
        EditGoalCommand goalCmd = getProjectCommandFactory().newEditGoalCommand();
        goalCmd.setEditedBy(admin);
        goalCmd.setGoalContainer(project);
        goalCmd.setName("Support concurrent users");
        goalCmd.setText("The systm must allow multiple concurrent users.");
        goalCmd = getCommandHandler().execute(goalCmd);
        Goal goal = goalCmd.getGoal();

        // Create a LexicalIssue for the misspelled word "systm" in the "Text" property
        EditLexicalIssueCommand lexCmd = getAnnotationCommandFactory().newEditLexicalIssueCommand();
        lexCmd.setEditedBy(admin);
        lexCmd.setGroupingObject(project);
        lexCmd.setAnnotatable(goal);
        lexCmd.setText("Possible misspelling: systm");
        lexCmd.setMustBeResolved(false);
        lexCmd.setWord("systm");
        // Property name drives reflection: get<Name>() / set<Name>() on the entity
        lexCmd.setAnnotatableEntityPropertyName("Text");
        lexCmd = getCommandHandler().execute(lexCmd);
        LexicalIssue lexicalIssue = (LexicalIssue) lexCmd.getIssue();

        // Create a ChangeSpellingPosition proposing the corrected word
        EditChangeSpellingPositionCommand spellingCmd =
                getAnnotationCommandFactory().newEditChangeSpellingPositionCommand();
        spellingCmd.setEditedBy(admin);
        spellingCmd.setIssue(lexicalIssue);
        spellingCmd.setText("Change 'systm' to 'system'");
        spellingCmd.setProposedWord("system");
        spellingCmd = getCommandHandler().execute(spellingCmd);

        // Resolve: the command rewrites the goal text then marks the issue resolved
        ResolveIssueCommand resolveCmd =
                getAnnotationCommandFactory().newResolveIssueCommand(spellingCmd.getPosition());
        resolveCmd.setEditedBy(admin);
        resolveCmd.setIssue(lexicalIssue);
        resolveCmd.setPosition(spellingCmd.getPosition());
        resolveCmd.setAnnotatable(goal);
        getCommandHandler().execute(resolveCmd);

        // Verify the goal text was corrected
        Goal reloaded = getProjectRepository()
                .findGoalByProjectOrDomainAndName(project, "Support concurrent users");
        assertFalse(reloaded.getText().contains("systm"),
                "misspelled word should have been replaced in the goal text");
        assertTrue(reloaded.getText().contains("system"),
                "corrected word should appear in the goal text");

        // Verify the issue is resolved
        Issue resolved = getAnnotationRepository().findIssue(project, goal,
                "Possible misspelling: systm");
        assertTrue(resolved.isResolved(), "lexical issue should be marked resolved");
    }

    // -------------------------------------------------------------------------
    // ResolveIssueWithAddWordToDictionaryPositionCommand
    // -------------------------------------------------------------------------

    @Test
    public void resolveIssueWithAddWordToDictionaryResolvesIssue() throws Exception {
        Project project = createProject("Annotation-add-word");
        Goal goal = createGoal(project, "Use domain-specific terminology");
        User admin = getUserRepository().findUserByUsername("admin");

        // Create a LexicalIssue for a word the spell-checker flagged as unknown
        EditLexicalIssueCommand lexCmd = getAnnotationCommandFactory().newEditLexicalIssueCommand();
        lexCmd.setEditedBy(admin);
        lexCmd.setGroupingObject(project);
        lexCmd.setAnnotatable(goal);
        lexCmd.setText("Unknown word: Requel");
        lexCmd.setMustBeResolved(false);
        lexCmd.setWord("Requel");
        lexCmd.setAnnotatableEntityPropertyName("Text");
        lexCmd = getCommandHandler().execute(lexCmd);
        LexicalIssue lexicalIssue = (LexicalIssue) lexCmd.getIssue();

        // Create an AddWordToDictionaryPosition
        EditAddWordToDictionaryPositionCommand addWordCmd =
                getAnnotationCommandFactory().newEditAddWordToDictionaryPositionCommand();
        addWordCmd.setEditedBy(admin);
        addWordCmd.setIssue(lexicalIssue);
        addWordCmd.setText("Add 'Requel' to the project dictionary");
        addWordCmd = getCommandHandler().execute(addWordCmd);

        // Resolve: the command adds the word to the dictionary then marks the issue resolved
        ResolveIssueCommand resolveCmd =
                getAnnotationCommandFactory().newResolveIssueCommand(addWordCmd.getPosition());
        resolveCmd.setEditedBy(admin);
        resolveCmd.setIssue(lexicalIssue);
        resolveCmd.setPosition(addWordCmd.getPosition());
        resolveCmd.setAnnotatable(goal);
        getCommandHandler().execute(resolveCmd);

        Issue resolved = getAnnotationRepository().findIssue(project, goal,
                "Unknown word: Requel");
        assertTrue(resolved.isResolved(),
                "lexical issue should be marked resolved after adding word to dictionary");
    }

    // -------------------------------------------------------------------------
    // ResolveIssueWithAddActorPositionCommand
    // -------------------------------------------------------------------------

    @Test
    public void resolveIssueWithAddActorCreatesActorInProject() throws Exception {
        Project project = createProject("Annotation-add-actor");
        Goal goal = createGoal(project, "Define system participants");
        User admin = getUserRepository().findUserByUsername("admin");

        // Create a LexicalIssue for a word flagged as a potential actor name
        EditLexicalIssueCommand lexCmd = getAnnotationCommandFactory().newEditLexicalIssueCommand();
        lexCmd.setEditedBy(admin);
        lexCmd.setGroupingObject(project);
        lexCmd.setAnnotatable(goal);
        lexCmd.setText("Potential actor: Submitter");
        lexCmd.setMustBeResolved(false);
        lexCmd.setWord("Submitter");
        lexCmd.setAnnotatableEntityPropertyName("Text");
        lexCmd = getCommandHandler().execute(lexCmd);
        LexicalIssue lexicalIssue = (LexicalIssue) lexCmd.getIssue();

        // Create an AddActorPosition proposing to add "Submitter" as an actor
        EditAddActorToProjectPositionCommand addActorCmd =
                getProjectCommandFactory().newEditAddActorToProjectPositionCommand();
        addActorCmd.setEditedBy(admin);
        addActorCmd.setIssue(lexicalIssue);
        addActorCmd.setProjectOrDomain(project);
        addActorCmd.setText("Add 'Submitter' as an actor to the project");
        addActorCmd = getCommandHandler().execute(addActorCmd);

        // Resolve: the command creates the actor then marks the issue resolved
        ResolveIssueCommand resolveCmd =
                getAnnotationCommandFactory().newResolveIssueCommand(addActorCmd.getPosition());
        resolveCmd.setEditedBy(admin);
        resolveCmd.setIssue(lexicalIssue);
        resolveCmd.setPosition(addActorCmd.getPosition());
        resolveCmd.setAnnotatable(goal);
        getCommandHandler().execute(resolveCmd);

        // Verify actor "Submitter" was created in the project
        Project reloaded = getProjectRepository().findProjectByName(project.getName());
        assertTrue(reloaded.getActors().stream()
                        .anyMatch(a -> "Submitter".equals(a.getName())),
                "resolving with AddActorPosition should create the actor in the project");

        Issue resolvedIssue = getAnnotationRepository().findIssue(project, goal,
                "Potential actor: Submitter");
        assertTrue(resolvedIssue.isResolved(), "issue should be marked resolved");
    }

    // -------------------------------------------------------------------------
    // ResolveIssueWithAddGlossaryTermPositionCommand
    // -------------------------------------------------------------------------

    @Test
    public void resolveIssueWithAddGlossaryTermCreatesGlossaryTermInProject() throws Exception {
        Project project = createProject("Annotation-add-glossary");
        Goal goal = createGoal(project, "Document domain terminology");
        User admin = getUserRepository().findUserByUsername("admin");

        // Create a LexicalIssue for a domain-specific word
        EditLexicalIssueCommand lexCmd = getAnnotationCommandFactory().newEditLexicalIssueCommand();
        lexCmd.setEditedBy(admin);
        lexCmd.setGroupingObject(project);
        lexCmd.setAnnotatable(goal);
        lexCmd.setText("Unknown term: Requel");
        lexCmd.setMustBeResolved(false);
        lexCmd.setWord("Requel");
        lexCmd.setAnnotatableEntityPropertyName("Text");
        lexCmd = getCommandHandler().execute(lexCmd);
        LexicalIssue lexicalIssue = (LexicalIssue) lexCmd.getIssue();

        // Create an AddGlossaryTermPosition proposing to add "Requel" to the glossary
        EditAddWordToGlossaryPositionCommand addGlossaryCmd =
                getProjectCommandFactory().newEditAddWordToGlossaryPositionCommand();
        addGlossaryCmd.setEditedBy(admin);
        addGlossaryCmd.setIssue(lexicalIssue);
        addGlossaryCmd.setProjectOrDomain(project);
        addGlossaryCmd.setText("Add 'Requel' to the project glossary");
        addGlossaryCmd = getCommandHandler().execute(addGlossaryCmd);

        // Resolve: the command creates the glossary term then marks the issue resolved
        ResolveIssueCommand resolveCmd =
                getAnnotationCommandFactory().newResolveIssueCommand(addGlossaryCmd.getPosition());
        resolveCmd.setEditedBy(admin);
        resolveCmd.setIssue(lexicalIssue);
        resolveCmd.setPosition(addGlossaryCmd.getPosition());
        resolveCmd.setAnnotatable(goal);
        getCommandHandler().execute(resolveCmd);

        // Verify "Requel" was added to the project glossary
        Project reloaded = getProjectRepository().findProjectByName(project.getName());
        assertTrue(reloaded.getGlossaryTerms().stream()
                        .anyMatch(t -> "Requel".equals(t.getName())),
                "resolving with AddGlossaryTermPosition should create the term in the project glossary");

        Issue resolvedIssue = getAnnotationRepository().findIssue(project, goal,
                "Unknown term: Requel");
        assertTrue(resolvedIssue.isResolved(), "issue should be marked resolved");
    }
}
