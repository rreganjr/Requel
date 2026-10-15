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

/**
 * A project's AI data-handling settings (issue #262), from
 * `GET /api/projects/{name}/data-handling`. Every setting is on until switched off.
 */
export interface ProjectDataHandlingDto {
  /** Whether project text may be sent to a remote AI provider (`egress.external`). */
  externalProviderAllowed: boolean;
  /** Redaction category id (credentials, email, phone, ssn, card) to whether it is masked. */
  redaction: Record<string, boolean>;
}
