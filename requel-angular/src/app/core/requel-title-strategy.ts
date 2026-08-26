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
import { Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

/**
 * Sets the document title from each route's static `title` (#142), formatted as `"<title> · Requel"`.
 * A route with no title falls back to the bare app name. Dynamic, param-derived titles are #154.
 */
@Injectable({ providedIn: 'root' })
export class RequelTitleStrategy extends TitleStrategy {
  constructor(private readonly title: Title) {
    super();
  }

  override updateTitle(snapshot: RouterStateSnapshot): void {
    // Prefer an entity name resolved onto the deepest route (artifact editors,
    // via artifactNameResolver) over the static route title (#154).
    const routeTitle = this.resolvedEntityName(snapshot) ?? this.buildTitle(snapshot);
    this.title.setTitle(routeTitle ? `${routeTitle} · Requel` : 'Requel');
  }

  private resolvedEntityName(snapshot: RouterStateSnapshot): string | null {
    let route = snapshot.root;
    while (route?.firstChild) route = route.firstChild;
    const value = route?.data?.['entityName'];
    return typeof value === 'string' && value.length ? value : null;
  }
}
