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
import { EntityReferenceDto } from './entity-reference';

export interface GlossaryTermDto {
  id: number;
  version: number;
  name: string;
  text: string | null;
  createdBy: string | null;
  canonicalTermId: number | null;
  canonicalTermName: string | null;
  alternateTerms: EntityReferenceDto[] | null;
  referers: EntityReferenceDto[] | null;
  /**
   * #348: per synonym, the referers whose name or text still says it. Set on the detail only
   * (null in the list); empty when nothing does.
   */
  synonymUses?: SynonymUseDto[] | null;
}

/** #348: a synonym (an old name) and the referers that still use it. */
export interface SynonymUseDto {
  synonymId: number;
  synonymName: string;
  referers: EntityReferenceDto[];
}

/** #348: what ReplaceTermText did with each referer it was given. */
export interface ReplaceTermTextResultDto {
  applied: EntityReferenceDto[];
  refused: EntityReferenceDto[];
  unchanged: EntityReferenceDto[];
}
