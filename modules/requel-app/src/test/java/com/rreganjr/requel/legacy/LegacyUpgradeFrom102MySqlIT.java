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
package com.rreganjr.requel.legacy;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Issue #380: Requel started on an upgraded Requel 1.0.2 database. */
@Testcontainers(disabledWithoutDocker = true)
public class LegacyUpgradeFrom102MySqlIT extends LegacyUpgradeAppMySqlIT {

	@Container
	static final MySQLContainer<?> MYSQL = mysql();

	@DynamicPropertySource
	static void fixture(DynamicPropertyRegistry registry) {
		useFixture(MYSQL, "1.0.2", registry);
	}

	@Override
	protected String version() {
		return "1.0.2";
	}
}
