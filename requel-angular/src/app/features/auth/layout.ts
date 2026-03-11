import { Component, computed } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Menubar } from 'primeng/menubar';
import { AuthService } from '../../core/auth.service';
import { MenuItem } from 'primeng/api';

/**
 * Main application layout with top menu bar and content area.
 * Wraps all authenticated routes.
 */
@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, Menubar],
  template: `
    <div class="layout">
      <p-menubar [model]="menuItems()">
        <ng-template #start>
          <span class="app-title">Requel</span>
        </ng-template>
      </p-menubar>
      <div class="layout-content">
        <router-outlet />
      </div>
    </div>
  `,
  styles: [`
    .layout { display: flex; flex-direction: column; min-height: 100vh; }
    .app-title { font-weight: 700; font-size: 1.25rem; margin-right: 1rem; }
    .layout-content { flex: 1; padding: 1rem; }
  `]
})
export class LayoutComponent {

  private readonly authService: AuthService;

  readonly menuItems = computed<MenuItem[]>(() => {
    const user = this.authService.user();
    const isAdmin = user?.roles?.includes('SystemAdminUserRole') ?? false;
    const items: MenuItem[] = [
      { label: 'Projects', icon: 'pi pi-folder', routerLink: '/projects' }
    ];

    if (isAdmin) {
      items.push({
        label: 'Admin',
        icon: 'pi pi-cog',
        items: [
          { label: 'Users', icon: 'pi pi-users', routerLink: '/users' }
        ]
      });
    }

    items.push({
      label: user?.name ?? user?.username ?? 'Account',
      icon: 'pi pi-user',
      items: [
        { label: 'Edit Account', icon: 'pi pi-cog', routerLink: '/account' },
        { separator: true },
        { label: 'Logout', icon: 'pi pi-sign-out', command: () => this.authService.logout() }
      ]
    });

    items.push({
      label: 'Help',
      icon: 'pi pi-question-circle',
      items: [
        { label: 'User Guide', icon: 'pi pi-book', url: '/doc/UserGuide.pdf', target: '_blank' }
      ]
    });

    return items;
  });

  constructor(authService: AuthService) {
    this.authService = authService;
  }
}
