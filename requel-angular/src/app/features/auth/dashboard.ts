import { Component, computed } from '@angular/core';
import { AuthService } from '../../core/auth.service';

/**
 * Placeholder dashboard shown after login. Will be replaced with
 * the project list / workspace view in Phase 1.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  template: `
    <h2>Welcome, {{ displayName() }}</h2>
    <p>Select a project from the sidebar to begin working on requirements.</p>
  `
})
export class DashboardComponent {
  readonly displayName = computed(() => {
    const user = this.authService.user();
    return user?.name ?? user?.username ?? 'User';
  });

  constructor(private authService: AuthService) {}
}
