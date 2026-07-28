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

/** A tag/category definition. Mirrors the backend TagDto. */
export interface TagDto {
  id: number;
  version: number;
  category: string | null;
  value: string | null;
  projectId: number | null;
  color: string | null;
  createdBy: string | null;
}

/** A reference to an entity a tag is assigned to. */
export interface TagEntityRef {
  entityType: string;
  entityId: number;
}

/** A typed category's rules (Phase 6). Empty allowedEntityTypes/values mean no restriction. */
export interface TagCategoryDto {
  id: number;
  version: number;
  projectId: number | null;
  name: string;
  exclusive: boolean;
  color: string | null;
  allowedEntityTypes: string[];
  values: string[];
}

/** Human display label for a tag: "category=value" when namespaced, else just "value". */
export function tagLabel(tag: Pick<TagDto, 'category' | 'value'>): string {
  return tag.category ? `${tag.category}=${tag.value}` : (tag.value ?? '');
}
