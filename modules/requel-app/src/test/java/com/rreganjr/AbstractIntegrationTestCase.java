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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import com.rreganjr.requel.user.impl.OrganizationImpl;
import junit.framework.AssertionFailedError;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.junit.Before;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.nlp.dictionary.NLPProcessorFactory;
import com.rreganjr.nlp.dictionary.DictionaryRepository;
import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.requel.Application;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.DomainAdminUserRole;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.nlp.dictionary.command.ImportDictionaryCommand;

import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.impl.repository.init.AdminUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.AssistantUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.ProjectUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.UserRolePermissionsInitializer;
import com.rreganjr.requel.project.impl.repository.init.StakeholderPermissionsInitializer;
import com.rreganjr.requel.user.command.EditUserCommand;
import com.rreganjr.requel.user.command.UserCommandFactory;
import com.rreganjr.requel.user.impl.AbstractUserRole;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.UserRole;
import com.rreganjr.requel.user.UserRolePermission;
import com.rreganjr.requel.user.exception.NoSuchUserException;

/**
 * Base test case for including Spring managed beans and transaction mgmt. in
 * tests.
 * 
 * @author ron
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTestCase extends AbstractJUnit4SpringContextTests {
	protected static final Logger log = Logger.getLogger(AbstractIntegrationTestCase.class);

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

	private static final AtomicBoolean baselineInitialized = new AtomicBoolean(false);

	protected AbstractIntegrationTestCase() {
		super();
		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("db.properties")) {
			if (inputStream == null) {
				throw new IllegalStateException("db.properties not found on the test classpath");
			}
			Properties dbProperties = new Properties();
			dbProperties.load(inputStream);
			initDatabase(dbProperties);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize test database", e);
		}
		//setPopulateProtectedVariables(true);
	}

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

    @Before
    public void initializeBaselineData() throws Exception {
        boolean first = baselineInitialized.compareAndSet(false, true);
        if (first) {
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
    protected void setUserCommandFactory(UserCommandFactory userCommandFactory) {
        this.userCommandFactory = userCommandFactory;
    }

    private void grantProjectRoleIfMissing(String username) throws Exception {
        User user = getUserRepository().findUserByUsername(username);
        boolean hasRole = user.getUserRoles().stream()
                .anyMatch(role -> role instanceof ProjectUserRole);
        if (!hasRole) {
            // Make sure mandatory fields survive the updateUser() pass.
            if (user.getOrganization() == null || user.getOrganization().getName() == null) {
                user.setOrganization(new OrganizationImpl("Requel"));
            }
            EditUserCommand cmd = userCommandFactory.newEditUserCommand();
            cmd.setEditedBy(user);
            cmd.setUser(user);
            cmd.setUsername(user.getUsername());
            cmd.setName(user.getName());
            cmd.setEmailAddress(user.getEmailAddress());
            cmd.setPhoneNumber(user.getPhoneNumber());
            cmd.setOrganizationName(user.getOrganization().getName());
            cmd.setEditable(user.isEditable());
            cmd.addUserRoleName(ProjectUserRole.getRoleName(ProjectUserRole.class));
            getCommandHandler().execute(cmd);
        }
    }

	@Autowired
	protected void setNlpProcessorFactory(NLPProcessorFactory nlpProcessorFactory) {
		this.nlpProcessorFactory = nlpProcessorFactory;
	}

	protected synchronized void ensureDictionaryLoaded() throws Exception {
		boolean hasWords = getDictionaryRepository().findWords() != null
				&& !getDictionaryRepository().findWords().isEmpty();
		if (hasWords) {
			return;
		}

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
		if (getDictionaryRepository().findWords() == null
				|| getDictionaryRepository().findWords().isEmpty()) {
			throw new IllegalStateException("Dictionary import completed but repository is still empty");
		}
	}

//	@Override
	protected void onSetUp() throws Exception {
//		super.onSetUp();
//		setDefaultRollback(false);
	}

//	@Override
	protected void onSetUpInTransaction() throws Exception {
//		super.onSetUpInTransaction();
//		DatabaseInitializer initializer = (DatabaseInitializer) getApplicationContext()
//				.getAutowireCapableBeanFactory().getBean("databaseInitializer");
//		initializer.initialize();
	}

	/**
	 * @return a File reference to the war directory of the instance
	 * @throws URISyntaxException
	 */
	/**
	 * Test whether two byte arrays are equal by comparing the byte value of
	 * each array element in the 'expected' array to the coresponding array
	 * element in the 'actual'.
	 * 
	 * @param expected
	 * @param actual
	 * @throws AssertionFailedError
	 */
	public static void assertEquals(byte[] expected, byte[] actual) throws AssertionFailedError {
		if ((expected != null) && (actual != null)) {
			if (expected.length == actual.length) {
				for (int i = 0; i < expected.length; i++) {
					if (expected[i] != actual[i]) {
						throw new AssertionFailedError("Expected " + expected[i] + " but found "
								+ actual[i] + " at position " + i);
					}
				}
				return;
			} else {
				throw new AssertionFailedError("The expected byte array was " + expected.length
						+ " bytes long, but the actual was " + actual.length + " bytes long.");
			}
		} else if ((expected != null) && (actual == null)) {
			throw new AssertionFailedError(
					"The expected byte array was not null, but the actual was null.");
		} else if ((expected == null) && (actual != null)) {
			throw new AssertionFailedError(
					"The expected byte array was null, but the actual was not null.");
		}
	}

	/**
	 * Test whether two collections contain the same values.
	 * 
	 * @param expected
	 * @param actual
	 * @throws AssertionFailedError
	 */
	public static void assertEquals(Collection<?> expected, Collection<?> actual)
			throws AssertionFailedError {
		if ((expected == null) && (actual == null)) {
			return;
		}
		if ((expected == null) || (actual == null)) {
			throw new AssertionFailedError("Expected collection " + expected
					+ " but found collection " + actual);
		}
		if (expected.size() != actual.size()) {
			throw new AssertionFailedError("Expected " + expected.size() + " entries but found "
					+ actual.size() + " entries.");
		}

		for (Object entry : expected) {
			if (!actual.contains(entry)) {
				throw new AssertionFailedError("Expected entry '" + entry
						+ "' but it was not found in collection " + actual);
			}
		}
		return;
	}

	/**
	 * Test whether the actual collection contains all the entries in the
	 * expected collection, although the actual may contain more.
	 * 
	 * @param expected
	 * @param actual
	 * @throws AssertionFailedError
	 */
	public static void assertContains(Collection<?> expected, Collection<?> actual)
			throws AssertionFailedError {
		if ((expected == null) && (actual == null)) {
			return;
		}
		if ((expected == null) || (actual == null)) {
			throw new AssertionFailedError("Expected collection " + expected
					+ " but found collection " + actual);
		}

		for (Object entry : expected) {
			if (!actual.contains(entry)) {
				throw new AssertionFailedError("Expected entry '" + entry
						+ "' but it was not found in collection " + actual);
			}
		}
		return;
	}

	/**
	 * Test whether a collection contains an expected value.
	 * 
	 * @param expected
	 * @param actual
	 * @throws AssertionFailedError
	 */
	public static void assertContains(Object expected, Collection<?> actual)
			throws AssertionFailedError {
		if (!actual.contains(expected)) {
			throw new AssertionFailedError("Expected '" + expected
					+ "' but it was not found in collection " + actual);
		}
	}

	/**
	 * assert that the keys and values in the 'expected' map are exactly equal
	 * to the keys and values in the 'actual' map.
	 * 
	 * @param expected
	 * @param actual
	 * @throws AssertionFailedError
	 */
	public static void assertEquals(Map<?, ?> expected, Map<?, ?> actual)
			throws AssertionFailedError {
		if ((expected == null) && (actual == null)) {
			return;
		}
		if (expected == null) {
			throw new AssertionFailedError("Expected null, but actual was Map<"
					+ actual.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ actual.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ "> " + "but actual was null.");
		} else if (actual == null) {
			throw new AssertionFailedError("Expected Map<"
					+ expected.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ expected.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ "> " + "but actual was null.");
		}
		if (expected.size() != actual.size()) {
			throw new AssertionFailedError("Expected Map<"
					+ expected.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ expected.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ "> " + "size " + expected.size() + "but actual was Map<"
					+ actual.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ actual.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ ">" + "size " + actual.size());
		}

		for (Object key : expected.keySet()) {
			if (!expected.get(key).equals(actual.get(key))) {
				throw new AssertionFailedError("Expected value '" + expected.get(key)
						+ "' for key '" + key + "' but found '" + actual.get(key) + "' instead.");
			}
		}
		return;
	}

	/**
	 * assert that the keys and values in the 'expected' map are also in the
	 * 'actual' map, but the 'actual' map may contain more properties.
	 * 
	 * @param expected
	 * @param actual
	 * @throws AssertionFailedError
	 */
	public static void assertContains(Map<?, ?> expected, Map<?, ?> actual)
			throws AssertionFailedError {
		if ((expected == null) && (actual == null)) {
			return;
		}
		if (expected == null) {
			throw new AssertionFailedError("Expected null, but actual was Map<"
					+ actual.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ actual.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ "> " + "but actual was null.");
		} else if (actual == null) {
			throw new AssertionFailedError("Expected Map<"
					+ expected.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ expected.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ "> " + "but actual was null.");
		}
		if (expected.size() > actual.size()) {
			throw new AssertionFailedError("Expected Map<"
					+ expected.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ expected.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ "> " + "size " + expected.size() + "but actual was Map<"
					+ actual.getClass().getTypeParameters()[0].getGenericDeclaration().getName()
					+ ","
					+ actual.getClass().getTypeParameters()[1].getGenericDeclaration().getName()
					+ ">" + "size " + actual.size());
		}

		for (Object key : expected.keySet()) {
			if (!expected.get(key).equals(actual.get(key))) {
				throw new AssertionFailedError("Expected value '" + expected.get(key)
						+ "' for key '" + key + "' but found '" + actual.get(key) + "' instead.");
			}
		}
		return;
	}

	/**
	 * Test that the supplied map contains the supplied key
	 * 
	 * @param key
	 * @param map
	 * @throws AssertionFailedError
	 */
	public static void assertContainsKey(Object key, Map<?, ?> map) throws AssertionFailedError {
		if (map == null) {
			throw new AssertionFailedError("The supplied map is null.");
		}
		if (!map.containsKey(key)) {
			throw new AssertionFailedError("The supplied map does not contain the expected key "
					+ key);
		}
	}

	private void initializePermissions(UserRepository userRepository) {
		log.debug("update role permissions...");
		for (Class<? extends UserRole> userRoleType : userRepository.findUserRoleTypes()) {
			Set<UserRolePermission> fixedPermissions = new HashSet<UserRolePermission>();
			for (UserRolePermission permission : AbstractUserRole
					.getAvailableUserRolePermissions(userRoleType)) {
				try {
					permission = userRepository.findUserRolePermission(userRoleType, permission
							.getName());
					log.debug(permission + " is already persistent.");
				} catch (EntityException e) {
					log.debug("creating: " + permission);
					permission = userRepository.persist(permission);
				}
				fixedPermissions.add(permission);
			}
			AbstractUserRole.userRoleTypePermissions.put(userRoleType, fixedPermissions);
		}
	}

	/**
	 * This method checks for the existance of the default admin user and
	 * creates it if doesn't exist.
	 * 
	 * @param userRepository
	 * @param userCommandFactory
	 * @param commandHandler
	 */
	private void initializeAdminUser(UserRepository userRepository,
			UserCommandFactory userCommandFactory, CommandHandler commandHandler) {
		try {
			userRepository.findUserByUsername("admin");
		} catch (NoSuchUserException e) {
			try {
				EditUserCommand command = userCommandFactory.newEditUserCommand();
				command.setUsername("admin");
				command.setPassword("admin");
				command.setRepassword("admin");
				command.setEmailAddress("rreganjr@users.sourceforge.net");
				command.setOrganizationName("Requel");
				command.addUserRoleName(SystemAdminUserRole.getRoleName(SystemAdminUserRole.class));
				commandHandler.execute(command);
			} catch (Exception e2) {
				log.error("failed to initialize the admin user: " + e2, e2);
			}
		}
	}

	/**
	 * this creates two test users, a "project" user with permission to create
	 * new projects and a "domain" admin user.
	 * 
	 * @param userRepository
	 * @param userCommandFactory
	 * @param commandHandler
	 */
	private void initializeTestUsers(UserRepository userRepository,
			UserCommandFactory userCommandFactory, CommandHandler commandHandler) {
		try {
			userRepository.findUserByUsername("project");
		} catch (NoSuchUserException e) {
			try {
				EditUserCommand command = userCommandFactory.newEditUserCommand();
				command.setUsername("project");
				command.setPassword("project");
				command.setRepassword("project");
				command.setEmailAddress("rreganjr@users.sourceforge.net");
				command.setOrganizationName("Requel");
				command.addUserRoleName(ProjectUserRole.getRoleName(ProjectUserRole.class));
				command.addUserRolePermissionName(ProjectUserRole
						.getRoleName(ProjectUserRole.class), ProjectUserRole.createProjects
						.getName());
				commandHandler.execute(command);
			} catch (Exception e2) {
				log.error("failed to initialize the project user: " + e2, e2);
			}
		}
		try {
			userRepository.findUserByUsername("domain");
		} catch (NoSuchUserException e) {
			try {
				EditUserCommand command = userCommandFactory.newEditUserCommand();
				command.setUsername("domain");
				command.setPassword("domain");
				command.setRepassword("domain");
				command.setEmailAddress("rreganjr@users.sourceforge.net");
				command.setOrganizationName("Requel");
				command.addUserRoleName(DomainAdminUserRole.getRoleName(DomainAdminUserRole.class));
				commandHandler.execute(command);
			} catch (Exception e2) {
				log.error("failed to initialize the domain user: " + e2, e2);
			}
		}
	}

	private void initDatabase(Properties dbProperties) {
		try {

			// make sure the driver is loaded
			Class.forName(dbProperties.getProperty("db.driver"));

			// create the full url from the properties
			StringBuilder jdbcUrlBuilder = new StringBuilder();
			jdbcUrlBuilder.append(dbProperties.getProperty("db.baseUrl"));
			jdbcUrlBuilder.append(dbProperties.getProperty("db.server"));
			jdbcUrlBuilder.append(":");
			jdbcUrlBuilder.append(dbProperties.getProperty("db.port"));
			jdbcUrlBuilder.append("/");

			// connection string without database
			String jdbcUrlNoDatabase = jdbcUrlBuilder.toString();

			jdbcUrlBuilder.append(dbProperties.getProperty("db.name"));
			jdbcUrlBuilder.append(dbProperties.getProperty("db.urlParams"));

			String jdbcUrl = jdbcUrlBuilder.toString();

			try {
				// try to connect to the database to see if it already exists
				DriverManager.getConnection(jdbcUrl, dbProperties.getProperty("db.username"),
						dbProperties.getProperty("db.password"));
			} catch (SQLException se) {
				// TODO: check the exception to see if it really was thrown
				// because the database doesn't exist
				// create the database
				Connection con = DriverManager.getConnection(jdbcUrlNoDatabase, dbProperties
						.getProperty("db.username"), dbProperties.getProperty("db.password"));
				Statement createDbStmt = con.createStatement();
				createDbStmt.execute("create database " + dbProperties.getProperty("db.name"));
			}
		} catch (ClassNotFoundException e) {
			// TODO: throw an exception, the app won't be available
			log.warn("cound not create database '" + dbProperties.getProperty("db.name")
					+ "', the driver class '" + dbProperties.getProperty("db.driver")
					+ "' in db.properties could not be loaded.", e);
		} catch (Exception e) {
			// TODO: throw an exception, the app won't be available
			if (dbProperties.getProperty("db.name") == null) {
				log.warn(
						"could not create database, the properties in db.properties could not be loaded: "
								+ e, e);
			} else {
				log.warn("could not create database '" + dbProperties.getProperty("db.name")
						+ "': " + e, e);
			}
		}
	}
}
