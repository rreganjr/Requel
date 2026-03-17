import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { LoginComponent } from './features/auth/login';
import { LayoutComponent } from './features/auth/layout';
import { DashboardComponent } from './features/auth/dashboard';
import { UserListComponent } from './features/users/user-list';
import { UserEditorComponent } from './features/users/user-editor';
import { EditAccountComponent } from './features/users/edit-account';
import { ProjectListComponent } from './features/projects/project-list';
import { ProjectEditorComponent } from './features/projects/project-editor';
import { SettingsComponent } from './features/users/settings';
import { StakeholderListComponent } from './features/stakeholders/stakeholder-list';
import { StakeholderEditorComponent } from './features/stakeholders/stakeholder-editor';
import { GoalListComponent } from './features/goals/goal-list';
import { GoalEditorComponent } from './features/goals/goal-editor';
import { StoryListComponent } from './features/stories/story-list';
import { StoryEditorComponent } from './features/stories/story-editor';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', component: DashboardComponent },
      { path: 'account', component: EditAccountComponent },
      { path: 'settings', component: SettingsComponent },
      { path: 'users', component: UserListComponent },
      { path: 'users/:username', component: UserEditorComponent },
      { path: 'projects', component: ProjectListComponent },
      { path: 'projects/:name', component: ProjectEditorComponent },
      { path: 'projects/:name/stakeholders', component: StakeholderListComponent },
      { path: 'projects/:name/stakeholders/:stakeholderId', component: StakeholderEditorComponent },
      { path: 'projects/:name/goals', component: GoalListComponent },
      { path: 'projects/:name/goals/:goalId', component: GoalEditorComponent },
      { path: 'projects/:name/stories', component: StoryListComponent },
      { path: 'projects/:name/stories/:storyId', component: StoryEditorComponent },
    ]
  },
  { path: '**', redirectTo: '' }
];
