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
/** A word in a project's dictionary or the installation-wide dictionary (issue #319). */
export interface DictionaryWordDto {
  id: number;
  lemma: string;
}

/**
 * An assistant finding a user ignored on one entity and property (issue #320). Listed on the
 * project's Dictionary page; removing it raises the finding again.
 */
export interface IgnoredFindingDto {
  id: number;
  /** What was ignored: a word, a phrase or a sentence snippet. */
  subject: string | null;
  /** e.g. unknown-word, vague-word, complex-text, glossary-term */
  findingType: string;
  /** e.g. Goal, Story, UseCase */
  entityType: string;
  entityId: number;
  entityName: string | null;
  /** Name, Text, or null for glossary terms. */
  propertyName: string | null;
  createdBy: string | null;
  dateCreated: string | null;
}
