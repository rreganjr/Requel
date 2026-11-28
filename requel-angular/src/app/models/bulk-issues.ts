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
/** #507: the actions BulkResolveIssues takes. */
export type BulkAction = 'IGNORE' | 'APPLY_FIX' | 'SET_SEVERITY' | 'MARK_ADVISORY';

export type BulkOutcome = 'DONE' | 'SKIPPED_NOT_APPLICABLE' | 'SKIPPED_AMBIGUOUS' | 'REFUSED' | 'FAILED';

/** What a bulk action did to one issue. */
export interface BulkIssueOutcome {
  issueId: number;
  outcome: BulkOutcome;
  reason?: string | null;
  positionId?: number | null;
}

/** The result of BulkResolveIssues: counts, and each issue's outcome in the order asked. */
export interface BulkResolveIssuesResult {
  action: BulkAction;
  done: number;
  skipped: number;
  refused: number;
  failed: number;
  outcomes: BulkIssueOutcome[];
}

/** How each action reads in the confirmation and the summary. */
export const BULK_ACTIONS: Record<BulkAction, { verb: string; explain: string; done: string }> = {
  IGNORE: { verb: 'Ignore', done: 'Ignored',
    explain: 'Resolves each issue that offers Ignore, so the assistant stops raising it on that item.' },
  APPLY_FIX: { verb: 'Apply suggested fix', done: 'Applied suggested fixes',
    explain: 'Resolves each issue that suggests exactly one change by making that change.' },
  SET_SEVERITY: { verb: 'Set severity', done: 'Set severity',
    explain: 'Changes the severity of every selected issue.' },
  MARK_ADVISORY: { verb: 'Mark advisory', done: 'Marked advisory',
    explain: 'Makes every selected issue advisory: it no longer has to be resolved.' },
};

export const BULK_OUTCOME_LABELS: Record<string, string> = {
  DONE: 'Done',
  SKIPPED_NOT_APPLICABLE: 'Skipped',
  SKIPPED_AMBIGUOUS: 'Skipped, more than one fix',
  REFUSED: 'Refused',
  FAILED: 'Failed',
};
