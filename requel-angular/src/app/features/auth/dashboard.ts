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
