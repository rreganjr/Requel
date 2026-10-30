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

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Issue #380: a refused or failed in-place upgrade, as a readable startup failure. */
public class LegacyUpgradeFailureAnalyzer extends AbstractFailureAnalyzer<LegacyUpgradeException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, LegacyUpgradeException cause) {
		String action = cause.isDatabaseChanged()
				? "Restore the database from the backup you took before upgrading, and report the message"
						+ " above at https://github.com/rreganjr/Requel/issues. Meanwhile, move the projects"
						+ " by XML export/import instead."
				: "Nothing was changed. This database was not made by an unmodified Requel 1.0.1, 1.0.2 or"
						+ " 1.1.0, so move its projects by XML export/import instead: export each project in"
						+ " the old version, start Requel on a new, empty database and import the files.";
		return new FailureAnalysis(cause.getMessage(), action + " See doc/guides/INSTALL.md, \"Upgrading\".",
				cause);
	}
}
