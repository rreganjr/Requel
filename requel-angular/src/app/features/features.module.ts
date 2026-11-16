import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectsModule } from './projects/projects.module';
import { UsersModule } from './users/users.module';
import { ReportsModule } from './reports/reports.module';
import { OpenIssuesModule } from './open-issues/open-issues.module';
import { IdeasModule } from './ideas/ideas.module';

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    ProjectsModule,
    UsersModule,
    ReportsModule,
    OpenIssuesModule,
    IdeasModule
  ]
})
export class FeaturesModule { }
