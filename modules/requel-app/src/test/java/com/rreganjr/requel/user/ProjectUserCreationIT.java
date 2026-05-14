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
package com.rreganjr.requel.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.rreganjr.requel.Application;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.requel.user.impl.repository.init.ProjectUserInitializer;

@SpringBootTest(classes = Application.class)
@TestPropertySource(locations = "classpath:db.properties", properties = {
        "db.name=requel_project_user_test",
        "db.driverUrl=jdbc:h2:mem:requel_project_user_test;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
})
@org.springframework.test.context.ActiveProfiles("test")
class ProjectUserCreationIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectUserInitializer projectUserInitializer;

    /**
     * The application's {@code DatabaseInitializationRunner} (an
     * {@code @EventListener(ApplicationReadyEvent.class)} component) runs every
     * registered {@code SystemInitializer} — including this one — during
     * context startup. By the time the test method executes, the "project"
     * user has already been created. The contract we still need to hold is
     * that the initializer is idempotent: re-running it on already-initialized
     * state must not throw, must not duplicate the user, and must leave the
     * existing record intact. (The runner can fire again on context refresh,
     * and tests may invoke initializers explicitly during their own setup.)
     */
    @Test
    void projectUserInitializerIsIdempotent() throws Exception {
        User initialUser = userRepository.findUserByUsername("project");
        assertThat(initialUser)
                .as("DatabaseInitializationRunner should have pre-created the project user")
                .isNotNull();
        Long initialId = initialUser.getId();

        projectUserInitializer.initialize();

        User reinitialized = userRepository.findUserByUsername("project");
        assertThat(reinitialized.getId())
                .as("re-running the initializer must not duplicate or replace the user")
                .isEqualTo(initialId);
    }
}
