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
import { Component, ChangeDetectionStrategy } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Route, Router, RouterLink } from '@angular/router';
import { filter, map, startWith } from 'rxjs';

interface Crumb {
  label: string;
  url: string;
  current: boolean;
}

/**
 * Top-bar breadcrumb (#154 chrome / #128 context). Builds the trail from the
 * current URL against the router config.
 *
 * #142 left the project routes FLAT (each `projects/:name/*` is its own leaf,
 * not a nested child), so the nested-ActivatedRoute walk the usual Angular
 * breadcrumb recipe relies on doesn't apply here. Instead we walk the URL
 * segment prefixes and match each against the shell's child routes, reading the
 * static `title` / `data.breadcrumb` for literal segments and the param value
 * for `:param` segments.
 *
 * Labels are dynamic for the project (the `:name` param IS the project's URL
 * name) and the section (from the list route's static title). An artifact
 * *editor* leaf shows its type ("Goal"), not the entity's name — resolving the
 * entity name by id is the #128 resolver step (PR3); a numeric id is never
 * shown as a crumb.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-breadcrumb',
  standalone: true,
  imports: [RouterLink],
  template: `
    @if (crumbs().length) {
      <nav aria-label="Breadcrumb" class="breadcrumb" data-testid="breadcrumb">
        <ol class="breadcrumb-list">
          @for (crumb of crumbs(); track crumb.url; let last = $last) {
            <li class="breadcrumb-item">
              @if (crumb.current) {
                <span class="breadcrumb-current" aria-current="page">{{ crumb.label }}</span>
              } @else {
                <a class="breadcrumb-link" [routerLink]="crumb.url">{{ crumb.label }}</a>
              }
              @if (!last) {
                <span class="breadcrumb-sep" aria-hidden="true">›</span>
              }
            </li>
          }
        </ol>
      </nav>
    }
  `,
  styles: [`
    :host { display: block; min-width: 0; }
    .breadcrumb-list {
      display: flex;
      align-items: center;
      flex-wrap: nowrap;
      gap: 0.25rem;
      margin: 0;
      padding: 0;
      list-style: none;
      min-width: 0;
      overflow: hidden;
    }
    .breadcrumb-item {
      display: inline-flex;
      align-items: center;
      gap: 0.25rem;
      min-width: 0;
    }
    .breadcrumb-link,
    .breadcrumb-current {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: 16ch;
      font-size: var(--rq-font-size-sm, 0.875rem);
    }
    .breadcrumb-link {
      color: var(--rq-header-fg);
      opacity: 0.85;
      text-decoration: none;
    }
    .breadcrumb-link:hover { opacity: 1; text-decoration: underline; }
    .breadcrumb-link:focus-visible {
      outline: 2px solid var(--rq-header-fg);
      outline-offset: 2px;
      border-radius: var(--rq-radius-sm);
    }
    .breadcrumb-current {
      color: var(--rq-header-fg);
      font-weight: var(--rq-font-weight-semibold, 600);
    }
    .breadcrumb-sep {
      color: var(--rq-header-fg);
      opacity: 0.6;
    }
  `]
})
export class BreadcrumbComponent {
  readonly crumbs;

  constructor(private readonly router: Router) {
    const url$ = this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.router.url),
      startWith(this.router.url),
    );
    this.crumbs = toSignal(url$.pipe(map(url => this.build(url))), {
      initialValue: this.build(this.router.url),
    });
  }

  private build(rawUrl: string): Crumb[] {
    const path = rawUrl.split('#')[0].split('?')[0];
    const segments = path.split('/').filter(Boolean);
    if (!segments.length) return [];

    const children = this.shellChildren();
    const crumbs: Crumb[] = [];

    for (let i = 1; i <= segments.length; i++) {
      const prefix = segments.slice(0, i);
      const route = this.match(children, prefix);
      if (!route) continue;

      const pattern = (route.path ?? '').split('/').filter(Boolean);
      const lastPattern = pattern[pattern.length - 1] ?? '';
      const value = safeDecode(prefix[i - 1]);
      const staticLabel = this.staticLabel(route);

      let label: string;
      if (lastPattern.startsWith(':')) {
        // Named segment: show the value when it reads as a name; never show a
        // bare numeric id — fall back to the route's type label (the entity
        // name is resolved in the #128 PR3 step).
        label = /^\d+$/.test(value) ? (staticLabel ?? value) : value;
      } else {
        label = staticLabel ?? value;
      }

      crumbs.push({ label, url: '/' + prefix.join('/'), current: false });
    }

    if (crumbs.length) {
      crumbs[crumbs.length - 1].current = true;

      // Upgrade the leaf from its type label ("Goal") to the entity's name
      // ("Login flow") when an editor route resolved one (#154 resolver step).
      const entityName = this.leafEntityName();
      if (entityName) {
        crumbs[crumbs.length - 1] = { ...crumbs[crumbs.length - 1], label: entityName };
      }
    }
    return crumbs;
  }

  /** The auth-guarded shell's child routes (the flat app route table). */
  private shellChildren(): Route[] {
    const shell = this.router.config.find(r => r.path === '' && !!r.children);
    return shell?.children ?? [];
  }

  /**
   * The `entityName` resolved onto the deepest active route (artifact editors,
   * via artifactNameResolver). Read from the live snapshot, which carries the
   * resolved value by the NavigationEnd that triggers a rebuild.
   */
  private leafEntityName(): string | null {
    let route = this.router.routerState.snapshot.root;
    while (route.firstChild) route = route.firstChild;
    const value = route.data?.['entityName'];
    return typeof value === 'string' && value.length ? value : null;
  }

  /** Full-path match of a URL prefix against a flat route, `:param` = wildcard. */
  private match(children: Route[], prefix: string[]): Route | null {
    for (const child of children) {
      const pattern = (child.path ?? '').split('/').filter(Boolean);
      if (pattern.length !== prefix.length) continue;
      if (pattern.every((p, i) => p.startsWith(':') || p === prefix[i])) return child;
    }
    return null;
  }

  private staticLabel(route: Route): string | undefined {
    const data = route.data as { breadcrumb?: string } | undefined;
    if (data?.breadcrumb) return data.breadcrumb;
    return typeof route.title === 'string' ? route.title : undefined;
  }
}

function safeDecode(segment: string): string {
  try {
    return decodeURIComponent(segment);
  } catch {
    return segment;
  }
}
