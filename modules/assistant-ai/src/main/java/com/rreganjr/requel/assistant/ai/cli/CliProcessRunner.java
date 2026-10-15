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
package com.rreganjr.requel.assistant.ai.cli;

import java.io.IOException;

/**
 * The process boundary of the CLI provider: runs one command and returns what it wrote. The
 * default implementation is {@link ProcessBuilderCliProcessRunner}; unit tests substitute a fake
 * so no test ever needs a real {@code claude}/{@code codex} binary (#259).
 */
public interface CliProcessRunner {

	/**
	 * Run the invocation to completion, timeout or byte cap. Never throws for a non-zero exit, a
	 * timeout or oversize output — those are reported on the result for the caller to judge.
	 *
	 * @throws IOException if the process cannot be started
	 * @throws InterruptedException if the calling thread is interrupted while waiting
	 */
	CliProcessResult run(CliInvocation invocation) throws IOException, InterruptedException;
}
