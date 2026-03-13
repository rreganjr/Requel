import { Component, computed } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { AuthService } from '../../core/auth.service';
import { SidebarNavComponent } from '../../shared/sidebar-nav';
import { MenuItem } from 'primeng/api';

/**
 * Main application layout: fixed header + sidebar accordion + main content area.
 * See doc/UI_DESIGN_GUIDE.md sections 1-3.
 */
@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, ButtonModule, MenuModule, SidebarNavComponent],
  template: `
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
export class LayoutComponent {

  private readonly authService: AuthService;

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

  constructor(authService: AuthService) {
    this.authService = authService;
  }
}
