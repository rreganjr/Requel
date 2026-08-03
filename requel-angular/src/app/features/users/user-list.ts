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
import { Component, OnInit, TemplateRef, ViewChild, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { UserDto } from '../../models/user';
import { UserService } from '../../core/user.service';
import { ListPageComponent } from '../../shared/list-page';
import { AppDataTableComponent, DataTableColumn, RowAction } from '../../shared/app-data-table';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [ListPageComponent, AppDataTableComponent, ButtonModule],
  template: `
    <app-list-page title="Users" [showSearch]="false">
      <app-data-table [value]="users()" [columns]="columns" [loading]="loading()"
                      [rowActions]="rowActions" searchPlaceholder="Search users..."
                      [globalFilterFields]="['username', 'name', 'emailAddress', 'organizationName']"
                      testid="user-list" (rowClick)="onRowSelect({ data: $event })"
                      emptyTitle="No users yet"
                      emptyMessage="Add the people who can sign in to Requel."
                      emptyIcon="pi-users" emptyActionLabel="New User"
                      [showEmptyAction]="true" (emptyAction)="onNewUser()">
        <div toolbarActions>
          <p-button label="New User" icon="pi pi-plus" (onClick)="onNewUser()" />
        </div>
      </app-data-table>
    </app-list-page>

    <ng-template #rolesCell let-user>{{ user.roles.join(', ') }}</ng-template>
  `,
  styles: []
})
export class UserListComponent implements OnInit {

  readonly users = signal<UserDto[]>([]);
  readonly loading = signal(true);

  @ViewChild('rolesCell', { static: true }) rolesCell!: TemplateRef<{ $implicit: UserDto }>;
  columns: DataTableColumn<UserDto>[] = [];
  rowActions: RowAction<UserDto>[] = [
    { label: 'Open', icon: 'pi pi-eye', command: u => this.onRowSelect({ data: u }) }
  ];

  constructor(private userService: UserService, private router: Router) {}

  async ngOnInit(): Promise<void> {
    this.columns = [
      { field: 'username', header: 'Username', sortable: true },
      { field: 'name', header: 'Name', sortable: true },
      { field: 'emailAddress', header: 'Email', sortable: true },
      { field: 'organizationName', header: 'Organization', sortable: true },
      { field: 'roles', header: 'Roles', cellTemplate: this.rolesCell }
    ];
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
