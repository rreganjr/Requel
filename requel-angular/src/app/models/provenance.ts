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
 * Where an entity came from (issue #272), from
 * `GET /api/projects/{name}/entities/{type}/{id}/sources`. Read-only: sources are recorded by an
 * ingest through the gateway, never from the UI.
 */
export interface ExternalSourceDto {
  id: number;
  /** Lower-cased source family, e.g. `jira`. */
  system: string;
  /** The source's own identifier, exactly as recorded, e.g. `CON-3685`. */
  externalId: string;
  /** `URL` or `PATH`, or null when the source has no locator. */
  locatorType: 'URL' | 'PATH' | null;
  locator: string | null;
  title: string | null;
  contentHash: string | null;
  lastIngestedAt: string | null;
  /** Issue #273: what sort of document, lower-cased, e.g. `runbook`; null when not recorded. */
  kind: string | null;
  /** Issue #273: why the source matters to the project; null when not recorded. */
  note: string | null;
}

export interface EntitySourceLinkDto {
  id: number;
  source: ExternalSourceDto;
  /** `DERIVED_FROM` (the entity was built from it) or `CITES` (it refers to it, #273). */
  relation: 'DERIVED_FROM' | 'CITES' | string;
  entityType: string;
  entityId: number;
  entityName: string | null;
  /** The part of the source, e.g. `AC-4` or `p.12`; null for the whole source. */
  fragment: string | null;
  ingestedAt: string | null;
  /** The source has a newer version than the one this fragment was last seen in. */
  notInLatestSource: boolean;
  /** The entity changed in Requel since it was ingested. */
  editedSinceIngest: boolean;
}

/** Issue #273: a source named by its identity, for the ends of a precedence edge. */
export interface SourceRefDto {
  system: string;
  externalId: string;
  title: string | null;
}

/** Issue #273: `subordinate` defers to `superior` — where the two disagree, the superior wins. */
export interface SourceAuthorityDto {
  subordinate: SourceRefDto;
  superior: SourceRefDto;
  note: string | null;
}

/** Issue #273: one of a project's sources, with its link counts and direct precedence. */
export interface ProjectSourceDto {
  source: ExternalSourceDto;
  derivedCount: number;
  citedByCount: number;
  defersTo: SourceRefDto[];
  outranks: SourceRefDto[];
}

/** Issue #273: the project's sources and references, and every precedence edge between them. */
export interface ProjectSourcesDto {
  sources: ProjectSourceDto[];
  authority: SourceAuthorityDto[];
}
