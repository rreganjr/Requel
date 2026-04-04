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
export interface UserPreferencesDto {
  sidebarProjectLimit: number;
  sidebarProjectStaleness: string;
}

export const STALENESS_OPTIONS = [
  { label: '1 Month', value: 'ONE_MONTH' },
  { label: '3 Months', value: 'THREE_MONTHS' },
  { label: '6 Months', value: 'SIX_MONTHS' },
  { label: '9 Months', value: 'NINE_MONTHS' },
  { label: '12 Months', value: 'TWELVE_MONTHS' },
  { label: 'Always Show', value: 'ALWAYS' },
];
