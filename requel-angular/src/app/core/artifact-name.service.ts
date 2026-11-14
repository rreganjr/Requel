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
import { Injectable, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { NavigationStart, Router } from '@angular/router';
import { filter } from 'rxjs';

/**
 * Issue #486: the name of the entity an editor is showing, as it is now. The breadcrumb's last
 * crumb and the tab title take the entity's name from the route resolver when the page opens
 * (#154), so a rename left both on the old name. An editor sets the current name here (after
 * it loads or saves), and the breadcrumb and title use it for the page it was set on. It's
 * cleared when navigation starts, so it never leaks onto the next page.
 */
@Injectable({ providedIn: 'root' })
export class ArtifactNameService {
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  private readonly current = signal<{ url: string; name: string } | null>(null);

  constructor() {
    this.router.events.pipe(filter(event => event instanceof NavigationStart))
      .subscribe(() => this.current.set(null));
  }

  /** The editor on the current page shows an entity named `name`. */
  set(name: string): void {
    const trimmed = name?.trim();
    if (!trimmed) return;
    const url = this.router.url;
    const previous = this.current();
    if (previous?.url === url && previous.name === trimmed) return;
    this.current.set({ url, name: trimmed });
    this.title.setTitle(`${trimmed} · Requel`);
  }

  /** The name set for the page at `url`, or null. Reactive. */
  nameFor(url: string): string | null {
    const current = this.current();
    return current && current.url === url ? current.name : null;
  }
}
