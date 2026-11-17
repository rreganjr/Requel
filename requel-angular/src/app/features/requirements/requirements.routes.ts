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
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
import { Routes } from '@angular/router';
import { RequirementListComponent } from './requirement-list';
import { RequirementEditorComponent } from './requirement-editor';
import { IdeaEditorComponent } from '../ideas/idea-editor';

export const REQUIREMENTS_ROUTES: Routes = [
  {
    path: 'projects/:name/requirements',
    children: [
      { path: '', component: RequirementListComponent },
      { path: 'new', component: RequirementEditorComponent },
      { path: ':requirementId', component: RequirementEditorComponent },
      { path: ':requirementId/edit', component: RequirementEditorComponent },
    ]
  },
  {
    path: 'projects/:name/ideas',
    children: [
      { path: 'new', component: IdeaEditorComponent },
      { path: ':ideaId', component: IdeaEditorComponent },
      { path: ':ideaId/edit', component: IdeaEditorComponent },
    ]
  }
];
