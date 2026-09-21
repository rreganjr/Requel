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
import com.rreganjr.nlp.dictionary.ProjectDictionaryWord;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.user.User;

/**
 * The project-scoped dictionary layer (issue #313).
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
