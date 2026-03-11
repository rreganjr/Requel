import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { LoginComponent } from './features/auth/login';
import { LayoutComponent } from './features/auth/layout';
import { DashboardComponent } from './features/auth/dashboard';
import { UserListComponent } from './features/users/user-list';
import { UserEditorComponent } from './features/users/user-editor';
import { EditAccountComponent } from './features/users/edit-account';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', component: DashboardComponent },
      { path: 'account', component: EditAccountComponent },
      { path: 'users', component: UserListComponent },
      { path: 'users/:username', component: UserEditorComponent },
      // Phase 2+ routes added here: projects, goals, stories, actors, etc.
    ]
  },
  { path: '**', redirectTo: '' }
];
