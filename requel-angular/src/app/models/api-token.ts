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

/** Personal access token metadata (never the secret). Mirrors the server ApiTokenDto (#73). */
export interface ApiTokenDto {
  id: number;
  name: string;
  createdAt: string;
  lastUsedAt: string | null;
  expiresAt: string | null;
  status: 'ACTIVE' | 'EXPIRED' | 'REVOKED';
}

export interface CreateApiTokenRequest {
  name: string;
  expiresInDays?: number | null;
}

/** The one-time plaintext token plus its metadata, returned by create. */
export interface CreateApiTokenResponse {
  token: string;
  tokenInfo: ApiTokenDto;
}
