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
package com.rreganjr.requel.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.rreganjr.requel.Application;
import com.rreganjr.requel.project.ProjectUserRole;

/**
 * #392: on a fresh install the built-in admin can manage personal access tokens without first
 * editing its own permissions. Boots the real application so the startup initializers run as they
 * do on a new database.
 */
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(locations = "classpath:db.properties", properties = {
		"db.name=requel_admin_tokens_test",
		"db.driverUrl=jdbc:h2:mem:requel_admin_tokens_test;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
})
@ActiveProfiles("test")
class AdminApiTokenPermissionIT {

	@Autowired
	private UserRepository users;

	@Test
	@Transactional
	void theBuiltInAdminCanManageApiTokensOnAFreshInstall() {
		User admin = users.findUserByUsername("admin");
		assertThat(admin.hasRole(ProjectUserRole.class)).isTrue();
		ProjectUserRole role = admin.getRoleForType(ProjectUserRole.class);
		assertThat(role.canCreateProjects()).as("createProjects").isTrue();
		assertThat(role.canManageApiTokens()).as("manageApiTokens").isTrue();
	}
}
