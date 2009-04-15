/*
 * $Id: AbstractIntegrationTestCase.java,v 1.23 2009/01/24 23:18:09 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan;

import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import junit.framework.AssertionFailedError;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.jpa.AbstractJpaTests;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.nlp.NLPProcessorFactory;
import edu.harvard.fas.rregan.nlp.dictionary.DictionaryRepository;
import edu.harvard.fas.rregan.nlp.dictionary.command.DictionaryCommandFactory;
import edu.harvard.fas.rregan.repository.DatabaseInitializer;
import edu.harvard.fas.rregan.repository.EntityException;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.project.DomainAdminUserRole;
import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.ProjectUserRole;
import edu.harvard.fas.rregan.requel.project.command.ProjectCommandFactory;
import edu.harvard.fas.rregan.requel.user.AbstractUserRole;
import edu.harvard.fas.rregan.requel.user.SystemAdminUserRole;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.UserRole;
import edu.harvard.fas.rregan.requel.user.UserRolePermission;
import edu.harvard.fas.rregan.requel.user.command.EditUserCommand;
import edu.harvard.fas.rregan.requel.user.command.UserCommandFactory;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchUserException;

/**
 * Base test case for including Spring managed beans and transaction mgmt. in
 * tests.
 * 
 * @author ron
 */
public abstract class AbstractIntegrationTestCase extends AbstractJpaTests {
	protected static final Logger log = Logger.getLogger(AbstractIntegrationTestCase.class);

	private ProjectCommandFactory projectCommandFactory;
	private UserCommandFactory userCommandFactory;
	private DictionaryCommandFactory dictionaryCommandFactory;
	private AnnotationCommandFactory annotationCommandFactory;
	private UserRepository userRepository;
	private ProjectRepository projectRepository;
	private DictionaryRepository dictionaryRepository;
	private AnnotationRepository annotationRepository;
	private CommandHandler commandHandler;
	private NLPProcessorFactory nlpProcessorFactory;

	protected AbstractIntegrationTestCase() {
		super();
		File webInfDir = new File(getInstanceBaseDirectory(), "WEB-INF");
		File classesDir = new File(webInfDir, "classes");
		File databasePropertiesFile = new File(classesDir, "db.properties");
		initDatabase(databasePropertiesFile);
		setPopulateProtectedVariables(true);
	}

	protected DictionaryCommandFactory getDictionaryCommandFactory() {
		return dictionaryCommandFactory;
	}

	@Autowired
	protected void setDictionaryCommandFactory(DictionaryCommandFactory dictionaryCommandFactory) {
		this.dictionaryCommandFactory = dictionaryCommandFactory;
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

	@Autowired
	protected void setNlpProcessorFactory(NLPProcessorFactory nlpProcessorFactory) {
		this.nlpProcessorFactory = nlpProcessorFactory;
	}

	@Override
	protected void onSetUp() throws Exception {
		super.onSetUp();
		setDefaultRollback(false);
	}

	@Override
	protected void onSetUpInTransaction() throws Exception {
		super.onSetUpInTransaction();
		DatabaseInitializer initializer = (DatabaseInitializer) getApplicationContext()
				.getAutowireCapableBeanFactory().getBean("databaseInitializer");
		initializer.initialize();
	}

	// TODO: this is what I wanted to do, but it won't work so I had to move the
	// db.properties
	// into the classpath >:(
	protected Object[] getPropertyLocations() {
		File webInfDir = new File(getInstanceBaseDirectory(), "WEB-INF");
		File databasePropertiesFile = new File(webInfDir, "db.properties");
		return new Resource[] { new FileSystemResource(databasePropertiesFile.toURI().toString()) };
	}

	@Override
	protected String[] getConfigLocations() {
		File webInfDir = new File(getInstanceBaseDirectory(), "WEB-INF");
		return new String[] { new File(webInfDir, "assistantConfig.xml").toURI().toString(),
				new File(webInfDir, "commandHandlerConfig.xml").toURI().toString(),
				new File(webInfDir, "lemmatizerConfig.xml").toURI().toString(),
				new File(webInfDir, "testConfig.xml").toURI().toString(),
				new File(webInfDir, "uiFrameworkConfig.xml").toURI().toString(),
				new File(webInfDir, "uiAnnotationConfig.xml").toURI().toString(),
				new File(webInfDir, "uiProjectConfig.xml").toURI().toString(),
				new File(webInfDir, "uiUserConfig.xml").toURI().toString(),
				new File(webInfDir, "uiNLPConfig.xml").toURI().toString(),
				new File(webInfDir, "uiMainConfig.xml").toURI().toString() };
	}

	/**
	 * @return a File reference to the war directory of the instance
	 * @throws URISyntaxException
	 */
	protected File getInstanceBaseDirectory() {
		try {
			URL log4jConfigPath = getClass().getClassLoader().getResource("log4j.properties");
			File log4jConfigFile = new File(log4jConfigPath.toURI()); // the
			// file up one to classes, one to WEB-INF, one to the root
			return log4jConfigFile.getParentFile().getParentFile().getParentFile();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

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
				command.setEmailAddress("rreganjr@acm.org");
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
				command.setEmailAddress("rreganjr@acm.org");
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
				command.setEmailAddress("rreganjr@acm.org");
				command.setOrganizationName("Requel");
				command.addUserRoleName(DomainAdminUserRole.getRoleName(DomainAdminUserRole.class));
				commandHandler.execute(command);
			} catch (Exception e2) {
				log.error("failed to initialize the domain user: " + e2, e2);
			}
		}
	}

	private void initDatabase(File databasePropertiesFile) {
		Properties dbProperties = new Properties();
		try {
			FileInputStream fis = new FileInputStream(databasePropertiesFile);
			dbProperties.load(fis);

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
