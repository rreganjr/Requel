import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { UserDto } from '../../models/user';
import { UserService } from '../../core/user.service';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [TableModule, ButtonModule],
  template: `
    <div class="user-list">
      <div class="header">
        <h2>Users</h2>
        <p-button label="New User" icon="pi pi-plus" (onClick)="onNewUser()" />
      </div>

      <p-table [value]="users()" [loading]="loading()" [paginator]="true" [rows]="20"
               [rowHover]="true" selectionMode="single" (onRowSelect)="onRowSelect($event)">
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
          <tr [pSelectableRow]="user">
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
    </div>
  `,
  styles: [`
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    h2 { margin: 0; }
  `]
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
