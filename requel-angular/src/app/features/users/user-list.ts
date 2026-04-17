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
import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { UserDto } from '../../models/user';
import { UserService } from '../../core/user.service';
import { ListPageComponent } from '../../shared/list-page';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [ListPageComponent, TableModule, ButtonModule],
  template: `
    <app-list-page title="Users" searchPlaceholder="Search users..."
                   (search)="dt.filterGlobal($event, 'contains')">
      <ng-container actions>
        <p-button label="New User" icon="pi pi-plus" (onClick)="onNewUser()" />
      </ng-container>

      <p-table #dt [value]="users()" [loading]="loading()" [paginator]="true" [rows]="20"
               [rowHover]="true" selectionMode="single" (onRowSelect)="onRowSelect($event)"
               [globalFilterFields]="['username', 'name', 'emailAddress', 'organizationName']">
        <ng-template #header>
          <tr>
            <th pSortableColumn="username">Username <p-sortIcon field="username" /></th>
            <th pSortableColumn="name">Name <p-sortIcon field="name" /></th>
            <th pSortableColumn="emailAddress">Email <p-sortIcon field="emailAddress" /></th>
            <th pSortableColumn="organizationName">Organization <p-sortIcon field="organizationName" /></th>
            <th>Roles</th>
          </tr>
        </ng-template>
        <ng-template #body let-user>
          <tr [pSelectableRow]="user" [attr.data-username]="user.username">
            <td>{{ user.username }}</td>
            <td>{{ user.name }}</td>
            <td>{{ user.emailAddress }}</td>
            <td>{{ user.organizationName }}</td>
            <td>{{ user.roles.join(', ') }}</td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr><td colspan="5">No users found.</td></tr>
        </ng-template>
      </p-table>
    </app-list-page>
  `,
  styles: []
})
export class UserListComponent implements OnInit {

  readonly users = signal<UserDto[]>([]);
  readonly loading = signal(true);

  constructor(private userService: UserService, private router: Router) {}

  async ngOnInit(): Promise<void> {
    try {
      const users = await this.userService.listUsers();
      this.users.set(users);
    } finally {
      this.loading.set(false);
    }
  }

  onNewUser(): void {
    this.router.navigate(['/users', 'new']);
  }

  onRowSelect(event: { data?: UserDto | UserDto[] }): void {
    const user = Array.isArray(event.data) ? event.data[0] : event.data;
    if (user) {
      this.router.navigate(['/users', user.username]);
    }
  }
}
