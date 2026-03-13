import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { UserDto } from '../../models/user';
import { UserService } from '../../core/user.service';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [FormsModule, TableModule, ButtonModule, InputText],
  template: `
    <div class="user-list">
      <div class="page-header">
        <h2>Users</h2>
        <div class="page-actions">
          <p-button label="New User" icon="pi pi-plus" (onClick)="onNewUser()" />
        </div>
      </div>

      <div class="search-bar">
        <span class="p-input-icon-left">
          <i class="pi pi-search"></i>
          <input pInputText [(ngModel)]="searchText" placeholder="Search users..."
                 (input)="dt.filterGlobal(searchText(), 'contains')" />
        </span>
      </div>

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
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .page-header h2 { margin: 0; }
    .search-bar { margin-bottom: 1rem; }
    .search-bar input { width: 300px; }
  `]
})
export class UserListComponent implements OnInit {

  readonly users = signal<UserDto[]>([]);
  readonly loading = signal(true);
  readonly searchText = signal('');

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
