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
import { Component, computed, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { ToastModule } from 'primeng/toast';
import { AuthService } from '../../core/auth.service';
import { EventStreamService } from '../../core/event-stream.service';
import { SidebarNavComponent } from '../../shared/sidebar-nav';
import { MenuItem, MessageService } from 'primeng/api';

/**
 * Main application layout: fixed header + sidebar accordion + main content area.
 * See doc/UI_DESIGN_GUIDE.md sections 1-3.
 */
@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, ButtonModule, MenuModule, ToastModule, SidebarNavComponent],
  providers: [MessageService],
  template: `
    <p-toast />
    <div class="layout">
      <header class="app-header">
        <a routerLink="/" class="header-brand">
          <img src="images/logo_robot.png" alt="Requel" class="header-logo" />
          <span class="header-title">REQUEL</span>
        </a>
        <div class="header-actions">
          <p-button [text]="true" icon="pi pi-bars"
                    [label]="'Menu'" class="account-trigger"
                    (onClick)="accountMenu.toggle($event)" />
          <p-menu #accountMenu [model]="accountMenuItems()" [popup]="true" />
        </div>
      </header>

      <div class="layout-body">
        <aside class="sidebar">
          <app-sidebar-nav />
        </aside>
        <main class="main-content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: [`
    .layout {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }

    .app-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 48px;
      padding: 0 1rem;
      background: #1a1a7e;
      color: #ffffff;
      flex-shrink: 0;
    }

    .header-brand {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      text-decoration: none;
      color: #ffffff;
    }

    .header-logo {
      height: 32px;
    }

    .header-title {
      font-weight: 700;
      font-size: 1.25rem;
      letter-spacing: 0.05em;
    }

    .header-actions {
      display: flex;
      align-items: center;
    }

    :host ::ng-deep .account-trigger .p-button {
      color: #ffffff;
    }

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
    }
  `]
})
export class LayoutComponent implements OnInit {

  private readonly authService: AuthService;
  private readonly eventStreamService: EventStreamService;

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

  constructor(authService: AuthService, eventStreamService: EventStreamService) {
    this.authService = authService;
    this.eventStreamService = eventStreamService;
  }

  ngOnInit(): void {
    // Open SSE connection, subscribed to the project broadcast channel so the
    // sidebar can reload counts whenever any project-scoped command completes.
    this.eventStreamService.connect(['Project:0']);
  }
}
