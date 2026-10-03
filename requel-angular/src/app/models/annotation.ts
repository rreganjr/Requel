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
export interface ArgumentDto {
  id: number;
  version: number;
  text: string;
  supportLevel: string;
  createdBy: string | null;
}

export interface PositionDto {
  id: number;
  version: number;
  text: string;
  createdBy: string | null;
  positionType: string;
  arguments: ArgumentDto[];
}

export interface NoteDto {
  id: number;
  version: number;
  text: string;
  createdBy: string | null;
  /** `ASSISTANT:<id>` when an assistant raised it, otherwise null (#270). */
  source?: string | null;
  /** An assistant raised it against text that has since changed: it may no longer apply (#270). */
  stale?: boolean;
  /** The assistant's display name when an assistant raised it (#265). */
  sourceName?: string | null;
  /** REVIEW, POLICY or LEXICAL when an assistant raised it (#265). */
  sourceKind?: 'REVIEW' | 'POLICY' | 'LEXICAL' | null;
}

/** Issue severity (#271). The server orders every issue list by it, HIGH first. */
export type IssueSeverity = 'LOW' | 'MEDIUM' | 'HIGH';

export interface IssueDto {
  id: number;
  version: number;
  text: string;
  mustBeResolved: boolean;
  severity: IssueSeverity;
  resolved: boolean;
  resolvedBy: string | null;
  resolvedByPosition: string | null;
  createdBy: string | null;
  positions: PositionDto[];
  /** `ASSISTANT:<id>` when an assistant raised it, otherwise null (#270). */
  source?: string | null;
  /** An assistant raised it against text that has since changed: it may no longer apply (#270). */
  stale?: boolean;
  /** The assistant's display name when an assistant raised it (#265). */
  sourceName?: string | null;
  /** REVIEW, POLICY or LEXICAL when an assistant raised it (#265). */
  sourceKind?: 'REVIEW' | 'POLICY' | 'LEXICAL' | null;
}

export interface AnnotationsDto {
  notes: NoteDto[];
  issues: IssueDto[];
}

export const ISSUE_SEVERITY_OPTIONS: { label: string; value: IssueSeverity }[] = [
  { label: 'High', value: 'HIGH' },
  { label: 'Medium', value: 'MEDIUM' },
  { label: 'Low', value: 'LOW' },
];

/** Ordering weight of a severity, 0 for a missing or unknown one; sort descending. */
export function severityRank(severity: string | null | undefined): number {
  switch (severity) {
    case 'HIGH': return 3;
    case 'MEDIUM': return 2;
    case 'LOW': return 1;
    default: return 0;
  }
}

/** Display label for a severity ('HIGH' -> 'High'). */
export function severityLabel(severity: string | null | undefined): string {
  return ISSUE_SEVERITY_OPTIONS.find(o => o.value === severity)?.label ?? (severity ?? '');
}

export const SUPPORT_LEVEL_OPTIONS = [
  { label: 'Strongly For', value: 'StronglyFor' },
  { label: 'For', value: 'For' },
  { label: 'Neutral', value: 'Neutral' },
  { label: 'Against', value: 'Against' },
  { label: 'Strongly Against', value: 'StronglyAgainst' },
];

/** Badge text for a stale note or issue (#270). */
export const STALE_LABEL = 'May no longer apply';

/** Tooltip on the stale badge (#270). */
export const STALE_TOOLTIP = 'An assistant raised this against text that has since changed, or a'
  + ' later analysis no longer reports it. Re-run analysis, or resolve it if it no longer applies.';
