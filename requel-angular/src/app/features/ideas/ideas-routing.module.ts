import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { IdeaInputComponent } from './idea-input/idea-input.component';

const routes: Routes = [
  {
    path: 'ideas/new',
    component: IdeaInputComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class IdeasRoutingModule { }
