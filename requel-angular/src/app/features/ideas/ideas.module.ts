import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { FileUploadModule } from 'primeng/fileupload';
import { IdeasRoutingModule } from './ideas-routing.module';
import { IdeaInputComponent } from './idea-input/idea-input.component';

@NgModule({
  declarations: [
    IdeaInputComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    IdeasRoutingModule,
    ButtonModule,
    InputTextModule,
    TextareaModule,
    FileUploadModule
  ]
})
export class IdeasModule { }
