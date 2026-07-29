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
package com.rreganjr;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.nlp.dictionary.PartOfSpeech;
import com.rreganjr.nlp.dictionary.impl.repository.NoSuchWordException;
import com.rreganjr.requel.Application;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.nlp.dictionary.command.ImportDictionaryCommand;

import com.rreganjr.requel.user.impl.repository.init.AdminUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.AssistantUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.ProjectUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.UserRolePermissionsInitializer;
import com.rreganjr.requel.project.impl.repository.init.StakeholderPermissionsInitializer;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.command.UserCommandFactory;

/**
 * Base test case for including Spring managed beans and transaction mgmt. in
 * tests.
 *
 * @author ron
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
// Pin the AI assistant off and the provider to noop for integration tests so the suite is
// deterministic regardless of any ambient REQUEL_AI_* environment variables on the developer's
// machine (OS env vars outrank application-test.properties, so this must be a @TestPropertySource).
// This only enforces the application's own shipped defaults. Tests that need the AI assistant on
// (e.g. AiReviewDispatchIT) override requel.ai.enabled via their own @TestPropertySource, which
// takes precedence over this inherited one.
@TestPropertySource(properties = { "requel.ai.enabled=false", "requel.ai.provider=noop" })
public abstract class AbstractIntegrationTestCase {
	protected static final Logger log = LoggerFactory.getLogger(AbstractIntegrationTestCase.class);

	private ProjectCommandFactory projectCommandFactory;
	private UserCommandFactory userCommandFactory;
	private AnnotationCommandFactory annotationCommandFactory;
	private UserRepository userRepository;
	private ProjectRepository projectRepository;
	private DictionaryRepository dictionaryRepository;
	private AnnotationRepository annotationRepository;
	private CommandHandler commandHandler;
	private NLPProcessorFactory nlpProcessorFactory;
	private AdminUserInitializer adminUserInitializer;
	private AssistantUserInitializer assistantUserInitializer;
	private ProjectUserInitializer projectUserInitializer;
	private UserRolePermissionsInitializer userRolePermissionsInitializer;
	private StakeholderPermissionsInitializer stakeholderPermissionsInitializer;
	private TestRoleGrantHelper testRoleGrantHelper;


	protected ProjectCommandFactory getProjectCommandFactory() {
		return projectCommandFactory;
	}

	@Autowired
	protected void setProjectCommandFactory(ProjectCommandFactory projectCommandFactory) {
		this.projectCommandFactory = projectCommandFactory;
	}

	protected UserCommandFactory getUserCommandFactory() {
		return userCommandFactory;
	}

	@Autowired
	protected void setUserCommandFactory(UserCommandFactory userCommandFactory) {
		this.userCommandFactory = userCommandFactory;
	}

	protected UserRepository getUserRepository() {
		return userRepository;
	}

	@Autowired
	protected void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	protected DictionaryRepository getDictionaryRepository() {
		return dictionaryRepository;
	}

	@Autowired
	protected void setDictionaryRepository(DictionaryRepository dictionaryRepository) {
		this.dictionaryRepository = dictionaryRepository;
	}

	protected AnnotationRepository getAnnotationRepository() {
		return annotationRepository;
	}

	@Autowired
	protected void setAnnotationRepository(AnnotationRepository annotationRepository) {
		this.annotationRepository = annotationRepository;
	}

	protected AnnotationCommandFactory getAnnotationCommandFactory() {
		return annotationCommandFactory;
	}

	@Autowired
	protected void setAnnotationCommandFactory(AnnotationCommandFactory annotationCommandFactory) {
		this.annotationCommandFactory = annotationCommandFactory;
	}

	protected ProjectRepository getProjectRepository() {
		return projectRepository;
	}

	@Autowired
	protected void setProjectRepository(ProjectRepository projectRepository) {
		this.projectRepository = projectRepository;
	}

	protected CommandHandler getCommandHandler() {
		return commandHandler;
	}

	@Autowired
	protected void setCommandHandler(CommandHandler commandHandler) {
		this.commandHandler = commandHandler;
	}

	protected NLPProcessorFactory getNlpProcessorFactory() {
		return nlpProcessorFactory;
	}

    @BeforeEach
    public void initializeBaselineData() throws Exception {
        // Seed baseline data for the CURRENT Spring context's database. Each test context gets
        // its own in-memory DB (jdbc:h2:mem:requel-${random.uuid}, per #43/#76), so this must run
        // per context, not once per JVM. The initializers are idempotent (check-then-create), so
        // re-running against an already-seeded context is just cheap existence checks. (A prior
        // JVM-wide AtomicBoolean guard left every context after the first unseeded -> intermittent
        // "No user for username 'project'" in the full suite.)
        if (userRolePermissionsInitializer != null) {
            userRolePermissionsInitializer.initialize();
        }
        if (stakeholderPermissionsInitializer != null) {
            stakeholderPermissionsInitializer.initialize();
        }
        if (adminUserInitializer != null) {
            adminUserInitializer.initialize();
        }
        if (assistantUserInitializer != null) {
            assistantUserInitializer.initialize();
        }
        if (projectUserInitializer != null) {
            projectUserInitializer.initialize();
        }
        // Always ensure the key users have ProjectUserRole; idempotent and cheap.
        try { grantProjectRoleIfMissing("admin"); } catch (Exception e) { log.warn("grant admin project role failed", e); }
        try { grantProjectRoleIfMissing("project"); } catch (Exception e) { log.warn("grant project user role failed", e); }
        try { grantProjectRoleIfMissing("assistant"); } catch (Exception e) { log.warn("grant assistant role failed", e); }
    }

	@Autowired
	protected void setAdminUserInitializer(AdminUserInitializer initializer) {
		this.adminUserInitializer = initializer;
	}

	@Autowired
	protected void setAssistantUserInitializer(AssistantUserInitializer initializer) {
		this.assistantUserInitializer = initializer;
	}

	@Autowired
	protected void setProjectUserInitializer(ProjectUserInitializer initializer) {
		this.projectUserInitializer = initializer;
	}

	@Autowired
	protected void setUserRolePermissionsInitializer(UserRolePermissionsInitializer initializer) {
		this.userRolePermissionsInitializer = initializer;
	}

	@Autowired
	protected void setStakeholderPermissionsInitializer(StakeholderPermissionsInitializer initializer) {
		this.stakeholderPermissionsInitializer = initializer;
	}

    @Autowired
    protected void setTestRoleGrantHelper(TestRoleGrantHelper testRoleGrantHelper) {
        this.testRoleGrantHelper = testRoleGrantHelper;
    }

    private void grantProjectRoleIfMissing(String username) {
        testRoleGrantHelper.grantProjectRoleIfMissing(username);
    }

    /**
     * Grant the per-user {@code manageApiTokens} permission (issue #85) on the user's
     * ProjectUserRole. Not granted by default (opt-in), so tests that exercise token management
     * must call this explicitly for the users that should be able to manage tokens.
     */
    protected void grantManageApiTokens(String username) {
        testRoleGrantHelper.grantManageApiTokensIfMissing(username);
    }

	@Autowired
	protected void setNlpProcessorFactory(NLPProcessorFactory nlpProcessorFactory) {
		this.nlpProcessorFactory = nlpProcessorFactory;
	}

	protected synchronized void ensureDictionaryLoaded() throws Exception {
		// Check for a word the NLP pipeline requires at init time. A non-empty
		// Word table is an unreliable proxy: annotation tests that resolve
		// "add word to dictionary" issues insert individual words without
		// importing the full dictionary, which would cause this check to return
		// early leaving StanfordNameEntityRecognizer unable to find 'person'.
		try {
			getDictionaryRepository().findWord("person", PartOfSpeech.NOUN);
			return; // full dictionary is present
		} catch (NoSuchWordException e) {
			// fall through to import
		}

		// The import is order-independent as of issue #80: Word.id now preserves the dictionary-
		// assigned id (see AssignedIdentityGenerator), so the composite sense -> word FKs resolve
		// regardless of any pre-existing rows or the IDENTITY counter position. No table reset is
		// needed here anymore. (Regression covered by DictionaryImportNonPristineIT.)
		ImportDictionaryCommand importDictionary =
				(ImportDictionaryCommand) applicationContext.getBean("importDictionaryCommand");
		InputStream in = getClass().getClassLoader()
				.getResourceAsStream("nlp/dictionary/dictionary.xml.gz");
		if (in == null) {
			throw new IllegalStateException("nlp/dictionary/dictionary.xml.gz not found on classpath");
		}
		importDictionary.setInputStream(new GZIPInputStream(in));
		getCommandHandler().execute(importDictionary);

		// verify load succeeded so tests fail fast with a clear message
		try {
			getDictionaryRepository().findWord("person", PartOfSpeech.NOUN);
		} catch (NoSuchWordException e) {
			throw new IllegalStateException("Dictionary import completed but 'person' is still not found", e);
		}
	}

	@Autowired
	protected org.springframework.context.ApplicationContext applicationContext;
}
