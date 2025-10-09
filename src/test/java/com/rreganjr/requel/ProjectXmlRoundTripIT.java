package com.rreganjr.requel;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.command.ExportProjectCommand;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.repository.init.StakeholderPermissionsInitializer;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.repository.init.AdminUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.AssistantUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.ProjectUserInitializer;
import com.rreganjr.requel.user.impl.repository.init.UserRolePermissionsInitializer;
import com.rreganjr.requel.user.exception.NoSuchUserException;

/**
 * Regression coverage that round-trips {@code doc/samples/Requel.xml} through
 * the import/export commands to make sure JAXB bindings remain compatible.
 */
@SpringBootTest(classes = Application.class)
@TestPropertySource(locations = "classpath:db.properties")
class ProjectXmlRoundTripIT {

	private static final String SAMPLE_PROJECT_NAME_PREFIX = "Regression Project ";
	private static final AtomicBoolean BASELINE_BOOTSTRAPPED = new AtomicBoolean(false);

	@Autowired
	private ProjectCommandFactory projectCommandFactory;

	@Autowired
	private CommandHandler commandHandler;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserRolePermissionsInitializer userRolePermissionsInitializer;

	@Autowired
	private StakeholderPermissionsInitializer stakeholderPermissionsInitializer;

	@Autowired
	private AdminUserInitializer adminUserInitializer;

	@Autowired
	private AssistantUserInitializer assistantUserInitializer;

	@Autowired
	private ProjectUserInitializer projectUserInitializer;

	@Value("classpath:samples/Requel.xml")
	private Resource sampleProjectXml;

	@Test
	@Transactional
	void importExportRoundTripKeepsProjectRoundTrippable() throws Exception {
		initializeBaselineData();

		User projectUser = ensureProjectUserExists();
		String uniqueProjectName = SAMPLE_PROJECT_NAME_PREFIX + UUID.randomUUID();

		Project importedProject;
		try (InputStream xmlStream = sampleProjectXml.getInputStream()) {
			ImportProjectCommand importCommand = projectCommandFactory.newImportProjectCommand();
			importCommand.setAnalysisEnabled(false);
			importCommand.setEditedBy(projectUser);
			importCommand.setName(uniqueProjectName);
			importCommand.setInputStream(xmlStream);

			importCommand = commandHandler.execute(importCommand);
			importedProject = importCommand.getProject();
		}

		assertThat(importedProject).as("Imported project").isNotNull();
		assertThat(importedProject.getName()).isEqualTo(uniqueProjectName);

		ByteArrayOutputStream exportedXml = new ByteArrayOutputStream();
		ExportProjectCommand exportCommand = projectCommandFactory.newExportProjectCommand();
		exportCommand.setProject(importedProject);
		exportCommand.setOutputStream(exportedXml);
		commandHandler.execute(exportCommand);

		byte[] exportedBytes = exportedXml.toByteArray();
		assertThat(exportedBytes).as("Exported XML payload").isNotEmpty();
		assertThat(new String(exportedBytes, StandardCharsets.UTF_8))
				.as("Exported XML content")
				.contains("<project");

		// Ensure the exported XML can be imported again under a different project name.
		try (ByteArrayInputStream reimportStream = new ByteArrayInputStream(exportedBytes)) {
			ImportProjectCommand reimportCommand = projectCommandFactory.newImportProjectCommand();
			reimportCommand.setAnalysisEnabled(false);
			reimportCommand.setEditedBy(projectUser);
			reimportCommand.setName(importedProject.getName() + " Reimport " + Instant.now().toEpochMilli());
			reimportCommand.setInputStream(reimportStream);
			commandHandler.execute(reimportCommand);
		}
	}

	private User ensureProjectUserExists() throws NoSuchUserException {
		return userRepository.findUserByUsername("project");
	}

	private void initializeBaselineData() {
		if (BASELINE_BOOTSTRAPPED.compareAndSet(false, true)) {
			userRolePermissionsInitializer.initialize();
			stakeholderPermissionsInitializer.initialize();
			adminUserInitializer.initialize();
			assistantUserInitializer.initialize();
			projectUserInitializer.initialize();
		}
		ensureUserHasProjectRole("admin");
		ensureUserHasProjectRole("assistant");
		ensureUserHasProjectRole("project");
	}

	private void ensureUserHasProjectRole(String username) {
		try {
			User user = userRepository.findUserByUsername(username);
			if (!user.hasRole(ProjectUserRole.class)) {
				user.grantRole(ProjectUserRole.class);
			}
			user.getRoleForType(ProjectUserRole.class)
					.grantUserRolePermission(ProjectUserRole.createProjects);
		} catch (NoSuchUserException ignored) {
			// optional user (e.g., assistant) may not exist in some test configurations
		}
	}

}
