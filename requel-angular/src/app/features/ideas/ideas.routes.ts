import { Routes } from '@angular/router';
import { routeData } from '../../core/route-data';

export const ideaRoutes: Routes = [
  {
    path: 'ideas/new',
    loadComponent: () => import('./idea-input/idea-input.component').then(m => m.IdeaInputComponent),
    title: 'Submit idea',
    data: routeData({ section: 'project', breadcrumb: 'Submit idea' }),
  },
  {
    path: 'ideas/:project/:id',
    loadComponent: () => import('./idea-review/idea-review.component').then(m => m.IdeaReviewComponent),
    title: 'Review requirements',
    data: routeData({ section: 'project', breadcrumb: 'Review requirements' }),
  },
];
