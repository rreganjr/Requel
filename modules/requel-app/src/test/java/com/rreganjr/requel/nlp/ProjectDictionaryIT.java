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
package com.rreganjr.requel.nlp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.nlp.dictionary.InstallDictionaryWord;
import com.rreganjr.nlp.dictionary.ProjectDictionaryWord;
import com.rreganjr.nlp.dictionary.Word;
import com.rreganjr.requel.annotation.command.EditAddWordToDictionaryPositionCommand;
import com.rreganjr.requel.annotation.command.EditLexicalIssueCommand;
import com.rreganjr.requel.annotation.command.ResolveIssueCommand;
import com.rreganjr.requel.annotation.impl.LexicalIssue;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.user.User;

/**
 * The project-scoped dictionary layer (issue #313), and the installation-wide layer and the
 * WordNet regression fix (issue #319).
 * <p>
 * Before this, every word a user added through "Add to Dictionary" went into the installation-wide
 * WordNet {@code word} table, so a word added while working in one project was spelled-correctly
 * in every project on the installation. These cover the layer that replaced it: a word is known
 * and suggestable in the project it was added to and nowhere else, and it goes away with the
 * project.
 * <p>
 * Every fixture is created by the test. {@code @SpringBootTest} contexts are cached and shared, so
 * earlier classes leave rows and auto-increment counters behind — nothing here may assume an id
 * value or an empty table.
 *
 * @author ron
 */
public class ProjectDictionaryIT extends AbstractIntegrationTestCase {

	/**
	 * A word invented for these tests. It must be absent from the jazzy classpath lists, and the
	 * WordNet table is empty under the test profile ({@code DictionarySQLInitializer} is disabled
	 * there), so an unknown word stays unknown unless a project dictionary says otherwise.
	 */
	private static final String INVENTED_WORD = "requelspeak";

	/** A near-miss of {@link #INVENTED_WORD}, for the suggestion path. */
	private static final String MISSPELLED_WORD = "requelspek";

	/** Held only by a {@code word} row the test persists, as the WordNet corpus would hold it. */
	private static final String WORDNET_ONLY_WORD = "requelnettle";

	/** A near-miss of {@link #WORDNET_ONLY_WORD}. */
	private static final String WORDNET_ONLY_MISSPELLED = "requelnetle";

	/**
	 * Added installation-wide by the tests that need it, and removed before they finish: an
	 * installation word is visible to every other test in the shared context.
	 */
	private static final String INSTALL_WORD = "requelwide";

	/** A near-miss of {@link #INSTALL_WORD}. */
	private static final String INSTALL_MISSPELLED = "requelwid";

	@Test
	public void wordAddedInOneProjectIsUnknownInAnother() throws Exception {
		Project projectA = createProject("dict-a");
		Project projectB = createProject("dict-b");

		assertFalse(getDictionaryRepository().isKnownWord(projectA.getId(), INVENTED_WORD),
				"the invented word should start out unknown in project A");

		getDictionaryRepository().addToDictionary(projectA.getId(), INVENTED_WORD);

		assertTrue(getDictionaryRepository().isKnownWord(projectA.getId(), INVENTED_WORD),
				"the word should be known in the project it was added to");
		assertFalse(getDictionaryRepository().isKnownWord(projectB.getId(), INVENTED_WORD),
				"the word should not leak into another project");
		assertFalse(getDictionaryRepository().isKnownWord(INVENTED_WORD),
				"the word should not reach the installation-wide dictionary");
	}

	/**
	 * The reason the layer is a jazzy user dictionary on a per-project SpellChecker rather than a
	 * "is this word in the project's table" check: a project word has to be offered as a spelling
	 * suggestion too, and only in its own project.
	 */
	@Test
	public void projectWordIsSuggestedOnlyInItsOwnProject() throws Exception {
		Project projectA = createProject("dict-suggest-a");
		Project projectB = createProject("dict-suggest-b");

		getDictionaryRepository().addToDictionary(projectA.getId(), INVENTED_WORD);

		List<String> inA = getDictionaryRepository().findSpellingSuggestions(projectA.getId(),
				MISSPELLED_WORD, 2);
		List<String> inB = getDictionaryRepository().findSpellingSuggestions(projectB.getId(),
				MISSPELLED_WORD, 2);

		assertTrue(inA.contains(INVENTED_WORD),
				"expected the project's own word among its suggestions, got " + inA);
		assertFalse(inB.contains(INVENTED_WORD),
				"another project's word should not be suggested, got " + inB);
	}

	/**
	 * Proves the cached per-project SpellChecker is evicted on write. Without eviction the first
	 * read would populate the cache and the word added after it would stay invisible.
	 */
	@Test
	public void addedWordIsVisibleImmediately() throws Exception {
		Project project = createProject("dict-cache");

		// Populate the cache first, then add.
		assertFalse(getDictionaryRepository().isKnownWord(project.getId(), INVENTED_WORD));
		getDictionaryRepository().addToDictionary(project.getId(), INVENTED_WORD);

		assertTrue(getDictionaryRepository().isKnownWord(project.getId(), INVENTED_WORD),
				"a word added after the project's checker was cached should still be known");
	}

	/**
	 * MySQL's default collation makes (project_id, lemma) case-insensitive; H2 under create-drop
	 * does not. The repository compares lower-cased so both agree on one row and on both spellings
	 * reading as known.
	 */
	@Test
	public void addingTheSameWordInAnotherCaseIsANoOp() throws Exception {
		Project project = createProject("dict-case");

		getDictionaryRepository().addToDictionary(project.getId(), "Requelcase");
		getDictionaryRepository().addToDictionary(project.getId(), "requelcase");
		getDictionaryRepository().addToDictionary(project.getId(), "REQUELCASE");

		List<ProjectDictionaryWord> words = getDictionaryRepository().findProjectWords(
				project.getId());
		assertEquals(1, words.size(), "expected one row for the word, got " + words);
		assertEquals("Requelcase", words.get(0).getLemma(),
				"the lemma should be stored in the case it was first entered");
		assertTrue(getDictionaryRepository().isKnownWord(project.getId(), "requelcase"));
		assertTrue(getDictionaryRepository().isKnownWord(project.getId(), "Requelcase"));
	}

	/**
	 * The repository half of "deleting a project removes only its own words". The delete step in
	 * DeleteProjectCommandImpl, and the cascade behaviour around it, are covered by
	 * DeleteProjectIT / DeleteProjectMySqlIT.
	 */
	@Test
	public void deletingAProjectsWordsLeavesOtherProjectsAlone() throws Exception {
		Project projectA = createProject("dict-delete-a");
		Project projectB = createProject("dict-delete-b");

		getDictionaryRepository().addToDictionary(projectA.getId(), INVENTED_WORD);
		getDictionaryRepository().addToDictionary(projectB.getId(), INVENTED_WORD);

		int deleted = getDictionaryRepository().deleteProjectWords(projectA.getId());

		assertEquals(1, deleted, "expected one word removed from project A");
		assertTrue(getDictionaryRepository().findProjectWords(projectA.getId()).isEmpty(),
				"project A's dictionary should be empty");
		assertFalse(getDictionaryRepository().isKnownWord(projectA.getId(), INVENTED_WORD),
				"the word should be unknown in project A again");
		assertTrue(getDictionaryRepository().isKnownWord(projectB.getId(), INVENTED_WORD),
				"project B should keep its own copy of the word");
	}

	/**
	 * A null project is "no project": the installation-wide layers alone, exactly as the
	 * project-less methods behave. The processors are reachable from paths that may not have a
	 * project, and they must not throw there.
	 */
	@Test
	public void aNullProjectFallsBackToTheInstallationWideDictionary() throws Exception {
		assertTrue(getDictionaryRepository().isKnownWord((Long) null, "system"),
				"a word in the jazzy lists should be known with no project");
		assertFalse(getDictionaryRepository().isKnownWord((Long) null, INVENTED_WORD),
				"an invented word should be unknown with no project");
		assertTrue(getDictionaryRepository().findProjectWords(null).isEmpty());
		assertEquals(0, getDictionaryRepository().deleteProjectWords(null));
	}

	/**
	 * The write path end to end (issue #313): resolving an "Add to Dictionary" position puts the
	 * word in that project's dictionary and leaves the installation-wide WordNet {@code word}
	 * table alone. Before this ticket the same resolution wrote a senseless row into {@code word},
	 * which is what made the addition installation-wide.
	 */
	@Test
	public void resolvingAddToDictionaryWritesOnlyTheProjectsDictionary() throws Exception {
		Project project = createProject("dict-resolve");
		User admin = getUserRepository().findUserByUsername("admin");
		Goal goal = createGoal(project, "goal-" + System.nanoTime(), "a goal mentioning "
				+ INVENTED_WORD);

		int wordRowsBefore = getDictionaryRepository().findWords().size();

		EditLexicalIssueCommand issueCmd = getAnnotationCommandFactory()
				.newEditLexicalIssueCommand();
		issueCmd.setEditedBy(admin);
		issueCmd.setGroupingObject(project);
		issueCmd.setAnnotatable(goal);
		issueCmd.setText("Possible misspelling: " + INVENTED_WORD);
		issueCmd.setMustBeResolved(false);
		issueCmd.setWord(INVENTED_WORD);
		issueCmd.setAnnotatableEntityPropertyName("Text");
		issueCmd = getCommandHandler().execute(issueCmd);
		LexicalIssue issue = (LexicalIssue) issueCmd.getIssue();

		EditAddWordToDictionaryPositionCommand positionCmd = getAnnotationCommandFactory()
				.newEditAddWordToDictionaryPositionCommand();
		positionCmd.setEditedBy(admin);
		positionCmd.setIssue(issue);
		positionCmd.setText("Add '" + INVENTED_WORD + "' to the project dictionary");
		positionCmd = getCommandHandler().execute(positionCmd);

		ResolveIssueCommand resolve = getAnnotationCommandFactory().newResolveIssueCommand(
				positionCmd.getPosition());
		resolve.setEditedBy(admin);
		resolve.setIssue(issue);
		resolve.setPosition(positionCmd.getPosition());
		getCommandHandler().execute(resolve);

		List<ProjectDictionaryWord> words = getDictionaryRepository().findProjectWords(
				project.getId());
		assertEquals(1, words.size(), "expected the resolved word in the project's dictionary, got "
				+ words);
		assertEquals(INVENTED_WORD, words.get(0).getLemma());
		assertTrue(getDictionaryRepository().isKnownWord(project.getId(), INVENTED_WORD),
				"the word should be known in this project after the resolve");
		assertEquals(wordRowsBefore, getDictionaryRepository().findWords().size(),
				"resolving must not add a row to the installation-wide word table");
	}

	/**
	 * Loading the WordNet corpus leaves project dictionaries alone (issue #313).
	 * <p>
	 * This is the XML corpus path — {@code ImportDictionaryCommand} via
	 * {@code ensureDictionaryLoaded()}. It writes the installation-wide {@code word},
	 * {@code category}, {@code synset} and {@code sense} tables and has no path to
	 * {@code project_dictionary_words}, which is the whole point of separating the layers; this
	 * pins it so that a future "reset the dictionary" step cannot quietly take project words with
	 * it. The MySQL dump path ({@code DictionarySQLInitializer}) writes the same corpus tables and
	 * additionally skips entirely unless the dictionary is empty.
	 */
	@Test
	public void loadingTheCorpusLeavesProjectWordsAlone() throws Exception {
		Project project = createProject("dict-corpus");
		getDictionaryRepository().addToDictionary(project.getId(), INVENTED_WORD);

		ensureDictionaryLoaded();

		assertEquals(1, getDictionaryRepository().findProjectWords(project.getId()).size(),
				"the corpus load must not touch the project's dictionary");
		assertTrue(getDictionaryRepository().isKnownWord(project.getId(), INVENTED_WORD),
				"the project word is still known after the corpus load");
	}

	/**
	 * The #313 regression (issue #319): a project checker was built from the jazzy lists and the
	 * project's own words only, so a word known to the WordNet {@code word} table alone was
	 * flagged as a misspelling in every project while reading as correct with no project.
	 * <p>
	 * The row is persisted directly, as the corpus would hold it (a runtime id, above the range the
	 * importer assigns), and removed afterwards so it cannot leak into the shared context.
	 */
	@Test
	public void aWordNetWordIsKnownAndSuggestedInAProject() throws Exception {
		Project project = createProject("dict-wordnet");
		Word word = getDictionaryRepository().persist(new Word(WORDNET_ONLY_WORD,
				getDictionaryRepository().generatePhoneticCode(WORDNET_ONLY_WORD)));
		try {
			assertTrue(getDictionaryRepository().isKnownWord(WORDNET_ONLY_WORD),
					"the word should be known with no project");
			assertTrue(getDictionaryRepository().isKnownWord(project.getId(), WORDNET_ONLY_WORD),
					"a WordNet word should be known inside a project too");
			List<String> suggestions = getDictionaryRepository().findSpellingSuggestions(
					project.getId(), WORDNET_ONLY_MISSPELLED, 2);
			assertTrue(suggestions.contains(WORDNET_ONLY_WORD),
					"expected the WordNet word among the project's suggestions, got " + suggestions);
		} finally {
			getDictionaryRepository().delete(word);
		}
	}

	/**
	 * An installation-wide word (issue #319) is known in every project and with no project, and
	 * removing it makes it unknown everywhere at once: the layer reads per lookup, so there is no
	 * cached checker to evict.
	 */
	@Test
	public void anInstallationWordIsKnownEverywhereUntilRemoved() throws Exception {
		Project projectA = createProject("dict-install-a");
		Project projectB = createProject("dict-install-b");
		// Populate both project checkers before the add, so the test also proves no eviction is
		// needed.
		assertFalse(getDictionaryRepository().isKnownWord(projectA.getId(), INSTALL_WORD));
		assertFalse(getDictionaryRepository().isKnownWord(projectB.getId(), INSTALL_WORD));

		InstallDictionaryWord added = getDictionaryRepository().addInstallWord(INSTALL_WORD, null);
		try {
			assertTrue(getDictionaryRepository().isKnownWord(INSTALL_WORD),
					"an installation word should be known with no project");
			assertTrue(getDictionaryRepository().isKnownWord(projectA.getId(), INSTALL_WORD),
					"an installation word should be known in project A");
			assertTrue(getDictionaryRepository().isKnownWord(projectB.getId(),
					INSTALL_WORD.toUpperCase()),
					"an installation word should be known in project B, in any case");
			assertTrue(getDictionaryRepository().findSpellingSuggestions(projectA.getId(),
					INSTALL_MISSPELLED, 2).contains(INSTALL_WORD),
					"an installation word should be suggested inside a project");
			assertTrue(getDictionaryRepository().findProjectWords(projectA.getId()).isEmpty(),
					"an installation word is not a project word");
			assertEquals(added.getId(), getDictionaryRepository().addInstallWord(
					INSTALL_WORD.toUpperCase(), null).getId(),
					"adding the same word in another case should return the existing row");
		} finally {
			assertTrue(getDictionaryRepository().deleteInstallWord(added.getId()));
		}

		assertFalse(getDictionaryRepository().isKnownWord(INSTALL_WORD),
				"a removed installation word should be unknown with no project");
		assertFalse(getDictionaryRepository().isKnownWord(projectA.getId(), INSTALL_WORD),
				"a removed installation word should be unknown in project A at once");
		assertFalse(getDictionaryRepository().isKnownWord(projectB.getId(), INSTALL_WORD),
				"a removed installation word should be unknown in project B at once");
		assertFalse(getDictionaryRepository().deleteInstallWord(added.getId()),
				"a second delete of the same id should remove nothing");
	}

	/**
	 * The project-less {@code addToDictionary(String)} writes the installation layer, not the
	 * WordNet table (issue #319).
	 */
	@Test
	public void theProjectlessAddWritesTheInstallationTableNotWordNet() throws Exception {
		int wordRowsBefore = getDictionaryRepository().findWords().size();
		getDictionaryRepository().addToDictionary(INSTALL_WORD);
		InstallDictionaryWord added = findInstallWord(INSTALL_WORD);
		try {
			assertTrue(added != null, "expected the word in the installation table");
			assertEquals(wordRowsBefore, getDictionaryRepository().findWords().size(),
					"the WordNet table must not gain a row");
		} finally {
			if (added != null) {
				getDictionaryRepository().deleteInstallWord(added.getId());
			}
		}
	}

	/**
	 * Removing one project word by id (issue #319) leaves the same spelling in another project,
	 * and an id from another project removes nothing: the project id is part of the match.
	 */
	@Test
	public void deletingOneProjectWordIsScopedToItsProject() throws Exception {
		Project projectA = createProject("dict-one-a");
		Project projectB = createProject("dict-one-b");
		getDictionaryRepository().addToDictionary(projectA.getId(), INVENTED_WORD);
		getDictionaryRepository().addToDictionary(projectA.getId(), "requelother");
		getDictionaryRepository().addToDictionary(projectB.getId(), INVENTED_WORD);
		ProjectDictionaryWord wordA = getDictionaryRepository().findProjectWord(projectA.getId(),
				INVENTED_WORD.toUpperCase());
		ProjectDictionaryWord wordB = getDictionaryRepository().findProjectWord(projectB.getId(),
				INVENTED_WORD);
		assertEquals(2, getDictionaryRepository().countProjectWords(projectA.getId()));

		assertFalse(getDictionaryRepository().deleteProjectWord(projectA.getId(), wordB.getId()),
				"project B's word id must not delete anything through project A");
		assertTrue(getDictionaryRepository().isKnownWord(projectB.getId(), INVENTED_WORD));

		assertTrue(getDictionaryRepository().deleteProjectWord(projectA.getId(), wordA.getId()));

		assertFalse(getDictionaryRepository().isKnownWord(projectA.getId(), INVENTED_WORD),
				"the removed word should be unknown in project A at once");
		assertTrue(getDictionaryRepository().isKnownWord(projectA.getId(), "requelother"),
				"the project's other word should stay");
		assertTrue(getDictionaryRepository().isKnownWord(projectB.getId(), INVENTED_WORD),
				"project B should keep its own copy of the word");
		assertEquals(1, getDictionaryRepository().countProjectWords(projectA.getId()));
		assertEquals(1, getDictionaryRepository().countProjectWords(projectB.getId()));
		assertEquals(0, getDictionaryRepository().countProjectWords(null));
	}

	private InstallDictionaryWord findInstallWord(String lemma) {
		for (InstallDictionaryWord word : getDictionaryRepository().findInstallWords()) {
			if (word.getLemma().equalsIgnoreCase(lemma)) {
				return word;
			}
		}
		return null;
	}

	private Goal createGoal(Project project, String name, String text) throws Exception {
		User admin = getUserRepository().findUserByUsername("admin");
		EditGoalCommand cmd = getProjectCommandFactory().newEditGoalCommand();
		cmd.setEditedBy(admin);
		cmd.setGoalContainer(project);
		cmd.setName(name);
		cmd.setText(text);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getGoal();
	}

	private Project createProject(String label) throws Exception {
		long ts = System.nanoTime();
		User admin = getUserRepository().findUserByUsername("admin");
		EditProjectCommand cmd = getProjectCommandFactory().newEditProjectCommand();
		cmd.setEditedBy(admin);
		cmd.setName(label + "-" + ts);
		cmd.setText("test project for " + label);
		cmd.setOrganizationName("ProjectDictionaryTestOrg-" + ts);
		cmd = getCommandHandler().execute(cmd);
		return cmd.getProject();
	}
}
