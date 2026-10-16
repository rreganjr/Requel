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
import { NavigationError } from '@angular/router';

/**
 * Issue #400: a tab left open across a server upgrade keeps running the old build. The first
 * time it opens a lazily loaded page it asks for a chunk the new jar doesn't have, and the
 * navigation fails ("Failed to fetch dynamically imported module"). Reloading at the URL being
 * opened loads the new build and that page.
 */

/** The messages Chrome, Safari and Firefox use when a lazy chunk can't be fetched. */
const STALE_CHUNK_MESSAGES = [
  /Failed to fetch dynamically imported module/i,
  /Importing a module script failed/i,
  /error loading dynamically imported module/i,
];

/** sessionStorage key holding the last URL reloaded for a stale chunk, and when. */
export const STALE_CHUNK_RELOAD_KEY = 'rq-stale-chunk-reload';

/**
 * A second failure for the same URL within this window means the chunk really is missing from
 * the current build: show the error instead of reloading again (no reload loop).
 */
export const STALE_CHUNK_RETRY_WINDOW_MS = 30_000;

export function isStaleChunkError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : typeof error === 'string' ? error : '';
  return STALE_CHUNK_MESSAGES.some(pattern => pattern.test(message));
}

export interface ReloadDeps {
  assign: (url: string) => void;
  storage: Pick<Storage, 'getItem' | 'setItem'> | null;
  now: () => number;
}

function browserDeps(): ReloadDeps {
  let storage: ReloadDeps['storage'] = null;
  try {
    storage = window.sessionStorage;
  } catch {
    // storage blocked: reloadOnStaleChunk then declines rather than risk a loop
  }
  return { assign: url => window.location.assign(url), storage, now: () => Date.now() };
}

/**
 * The router's navigation-error handler. Reloads once at the navigation's URL when the error is a
 * stale chunk; returns whether it did. Anything else is left to the router's normal handling.
 */
export function reloadOnStaleChunk(event: NavigationError, deps: ReloadDeps = browserDeps()): boolean {
  if (!isStaleChunkError(event.error) || !deps.storage) {
    return false;
  }
  const now = deps.now();
  try {
    const last = JSON.parse(deps.storage.getItem(STALE_CHUNK_RELOAD_KEY) ?? 'null') as
      | { url?: string; at?: number }
      | null;
    if (last?.url === event.url && typeof last.at === 'number' && now - last.at < STALE_CHUNK_RETRY_WINDOW_MS) {
      return false;
    }
    deps.storage.setItem(STALE_CHUNK_RELOAD_KEY, JSON.stringify({ url: event.url, at: now }));
  } catch {
    return false;
  }
  deps.assign(event.url);
  return true;
}
