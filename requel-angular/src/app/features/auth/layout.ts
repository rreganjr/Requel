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
import { Component, computed, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Location } from '@angular/common';
import { NavigationEnd, Router, RouterOutlet, RouterLink } from '@angular/router';
import { filter, map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { ToastModule } from 'primeng/toast';
import { AuthService } from '../../core/auth.service';
import { EventStreamService } from '../../core/event-stream.service';
import { SidebarNavComponent } from '../../shared/sidebar-nav';
import { MenuItem, MessageService } from 'primeng/api';

/**
 * localStorage key for whether the user has collapsed the whole sidebar (the
 * top-bar toggle). Distinct from `requel_sidebar_groups` (per-group
 * open/closed, owned by the sidebar) and `requel_sidebar_expanded_projects`
 * (the project tree, owned by the sidebar). Three independent concerns.
 */
const SIDEBAR_COLLAPSED_KEY = 'requel_sidebar_collapsed';

function loadSidebarCollapsed(): boolean {
  try {
    return localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === 'true';
  } catch {
    return false;
  }
}

function persistSidebarCollapsed(collapsed: boolean): void {
  try {
    localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(collapsed));
  } catch {
    // Storage may be unavailable (private mode, quota). The collapse state is
    // a UX nicety, not data - drop the persistence silently.
  }
}

/**
 * Main application layout: top bar (back + breadcrumb region on the left;
 * search / account / sidebar-toggle on the right) over a collapsible sidebar
 * and the main content canvas. See doc/128-154-app-shell-plan.md.
 *
 * The breadcrumb region is reserved here and rendered by <app-breadcrumb> in
 * the #128 dynamic-breadcrumb step; PR1 ships the region empty so no coarse
 * static labels are shown in the interim.
 */
@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, ButtonModule, MenuModule, ToastModule, SidebarNavComponent],
  providers: [MessageService],
  template: `
    <p-toast />
    <a class="skip-link" href="#main-content">Skip to content</a>
    <div class="layout" [class.sidebar-collapsed]="sidebarCollapsed()">
      <header class="app-header">
        <div class="header-left">
          <button type="button" class="icon-btn sidebar-toggle"
                  [attr.aria-expanded]="!sidebarCollapsed()"
                  aria-controls="app-sidebar"
                  [attr.aria-label]="sidebarCollapsed() ? 'Show sidebar' : 'Hide sidebar'"
                  data-testid="sidebar-toggle"
                  (click)="toggleSidebar()">
            <i class="pi pi-bars" aria-hidden="true"></i>
          </button>
          <a routerLink="/" class="header-brand" data-testid="header-brand">
            <img src="images/logo_robot.png" alt="Requel" class="header-logo" />
            <span class="header-title">REQUEL</span>
          </a>
          @if (showBack()) {
            <!-- Distinct a11y name from the editors' own "Back" buttons (back to
                 list) - this is browser-history back. Keeps getByRole name:'Back'
                 unambiguous for AT and e2e. -->
            <button type="button" class="icon-btn back-btn"
                    aria-label="Go to previous page" data-testid="back-button"
                    (click)="goBack()">
              <i class="pi pi-arrow-left" aria-hidden="true"></i>
            </button>
          }
          <!-- Breadcrumb region (filled by <app-breadcrumb> in the #128 step). -->
          <div class="breadcrumb-region" data-testid="breadcrumb-region"></div>
        </div>

        <div class="header-right">
          <button type="button" class="icon-btn search-placeholder"
                  disabled aria-label="Search (coming soon)"
                  title="Search (coming soon)" data-testid="header-search">
            <i class="pi pi-search" aria-hidden="true"></i>
          </button>
          <p-button [text]="true" icon="pi pi-bars"
                    [label]="'Menu'" class="account-trigger"
                    (onClick)="accountMenu.toggle($event)" />
          <p-menu #accountMenu [model]="accountMenuItems()" [popup]="true" />
        </div>
      </header>

      <div class="layout-body">
        <aside id="app-sidebar" class="sidebar" [hidden]="sidebarCollapsed()">
          <app-sidebar-nav />
        </aside>
        <main id="main-content" class="main-content" tabindex="-1">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: [`
    .skip-link {
      position: absolute;
      left: 0.5rem;
      top: -3rem;
      z-index: 1100;
      padding: 0.5rem 1rem;
      background: var(--rq-header-bg);
      color: var(--rq-header-fg);
      border-radius: 0 0 var(--rq-radius-sm) var(--rq-radius-sm);
      text-decoration: none;
      transition: top 0.15s ease-in-out;
    }
    .skip-link:focus {
      top: 0;
    }

    .layout {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }

    .app-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      height: 48px;
      padding: 0 1rem;
      background: var(--rq-header-bg);
      color: var(--rq-header-fg);
      flex-shrink: 0;
    }

    .header-left,
    .header-right {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      min-width: 0;
    }

    .header-left { flex: 1; }

    .breadcrumb-region {
      display: flex;
      align-items: center;
      min-width: 0;
      overflow: hidden;
    }

    .header-brand {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      text-decoration: none;
      color: var(--rq-header-fg);
    }

    .header-logo {
      height: 32px;
    }

    .header-title {
      font-weight: 700;
      font-size: 1.25rem;
      letter-spacing: 0.05em;
    }

    /* Inline icon buttons in the header brand bar. Colors read the header
       foreground token so the bar stays token-driven (no literals). */
    .icon-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      padding: 0;
      border: none;
      background: transparent;
      color: var(--rq-header-fg);
      border-radius: var(--rq-radius-sm);
      cursor: pointer;
    }
    .icon-btn:hover:not(:disabled) {
      background: rgba(255, 255, 255, 0.12);
    }
    .icon-btn:focus-visible {
      outline: 2px solid var(--rq-header-fg);
      outline-offset: 2px;
    }
    .icon-btn:disabled {
      opacity: 0.5;
      cursor: default;
    }

    /* .account-trigger .p-button color lives in global styles.scss (#126). */

    .layout-body {
      display: flex;
      flex: 1;
      overflow: hidden;
    }

    .sidebar {
      width: 280px;
      flex-shrink: 0;
      overflow-y: auto;
      border-right: 1px solid var(--p-surface-200);
      background: var(--p-surface-0);
    }

    .main-content {
      flex: 1;
      overflow-y: auto;
      padding: 1.5rem;
      /* Light blue-gray canvas; list/editor content sits on white cards. */
      background: var(--rq-canvas-bg);
    }
  `]
})
export class LayoutComponent implements OnInit {

  private readonly authService: AuthService;
  private readonly eventStreamService: EventStreamService;
  private readonly location: Location;
  private readonly router: Router;

  readonly sidebarCollapsed = signal<boolean>(loadSidebarCollapsed());

  /** Current URL, tracked so the back button hides at the shell root. */
  private readonly currentUrl;
  readonly showBack;

  readonly accountMenuItems = computed<MenuItem[]>(() => {
    const user = this.authService.user();
    return [
      {
        label: user?.name ?? user?.username ?? 'Account',
        items: [
          { label: 'Settings', icon: 'pi pi-cog', routerLink: '/settings' },
          { label: 'Edit Account', icon: 'pi pi-user-edit', routerLink: '/account' },
          { separator: true },
          { label: 'Logout', icon: 'pi pi-sign-out', command: () => this.authService.logout() }
        ]
      }
    ];
  });

  constructor(
    authService: AuthService,
    eventStreamService: EventStreamService,
    location: Location,
    router: Router
  ) {
    this.authService = authService;
    this.eventStreamService = eventStreamService;
    this.location = location;
    this.router = router;

    this.currentUrl = toSignal(
      this.router.events.pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        map(() => this.router.url)
      ),
      { initialValue: this.router.url }
    );
    // Hide back at the shell root ('/' dashboard); show it everywhere else.
    this.showBack = computed(() => {
      const url = this.currentUrl();
      return url !== '/' && url !== '';
    });
  }

  ngOnInit(): void {
    // Open SSE connection, subscribed to the project broadcast channel so the
    // sidebar can reload counts whenever any project-scoped command completes.
    this.eventStreamService.connect(['Project:0']);
  }

  toggleSidebar(): void {
    const next = !this.sidebarCollapsed();
    this.sidebarCollapsed.set(next);
    persistSidebarCollapsed(next);
  }

  goBack(): void {
    this.location.back();
  }
}
