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
import { inject, Injectable, OnDestroy } from '@angular/core';
import { LiveAnnouncer } from '@angular/cdk/a11y';

/**
 * Polite live-region announcer for async / background status (issue #140, WCAG 4.1.3).
 *
 * Wraps CDK {@link LiveAnnouncer} (a single visually-hidden `aria-live="polite"` region it
 * manages on the document) so status messages reach assistive tech without stealing focus or
 * flashing a toast. {@link announceThrottled} coalesces bursts — a flurry of SSE events for the
 * same key announces once immediately, then at most once more (the latest) at the window's end —
 * so continuous background updates don't overwhelm (WCAG 2.2.2).
 */
@Injectable({ providedIn: 'root' })
export class AnnouncerService implements OnDestroy {
  private readonly live = inject(LiveAnnouncer);
  private readonly timers = new Map<string, ReturnType<typeof setTimeout>>();
  private readonly pending = new Map<string, string>();

  /** Announce a message politely, immediately. */
  announce(message: string): void {
    void this.live.announce(message, 'polite');
  }

  /**
   * Announce immediately if this key is idle, then open a cooldown window; further calls within
   * the window are coalesced and the latest is announced once when the window closes.
   */
  announceThrottled(key: string, message: string, delayMs = 1500): void {
    if (this.timers.has(key)) {
      this.pending.set(key, message);
      return;
    }
    this.announce(message);
    this.timers.set(key, setTimeout(() => {
      this.timers.delete(key);
      const latest = this.pending.get(key);
      if (latest !== undefined) {
        this.pending.delete(key);
        this.announce(latest);
      }
    }, delayMs));
  }

  ngOnDestroy(): void {
    for (const t of this.timers.values()) {
      clearTimeout(t);
    }
    this.timers.clear();
    this.pending.clear();
  }
}
