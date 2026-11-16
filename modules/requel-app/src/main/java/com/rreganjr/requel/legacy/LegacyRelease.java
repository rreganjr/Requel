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

import java.util.List;

/**
 * Issue #380: the old Requel releases whose databases can be upgraded in place, each with the
 * schema shape it leaves (recorded from the published image, see scripts/legacy-fixtures.sh)
 * and the scripts that turn that shape into V1's, ending with the data fix-ups every release
 * needs (upgrade-data.sql).
 */
public enum LegacyRelease {

	V1_0_1("1.0.1", "db/legacy/upgrade-1.0.1-users.sql", "db/legacy/upgrade-1.0.sql",
			"db/legacy/upgrade-data.sql"),
	V1_0_2("1.0.2", "db/legacy/upgrade-1.0.sql", "db/legacy/upgrade-data.sql"),
	V1_1_0("1.1.0", "db/legacy/upgrade-1.1.sql", "db/legacy/upgrade-data.sql");

	/** V1's shape: what every upgrade script must produce before Flyway baselines at 1. */
	public static final String V1_SHAPE = "db/legacy/v1.shape";

	private final String version;
	private final List<String> scripts;

	LegacyRelease(String version, String... scripts) {
		this.version = version;
		this.scripts = List.of(scripts);
	}

	public String version() {
		return version;
	}

	public String shapeResource() {
		return "db/legacy/requel-" + version + ".shape";
	}

	public List<String> scripts() {
		return scripts;
	}
}
