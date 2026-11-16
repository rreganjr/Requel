import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { IdeaService } from '../idea.service';
import { IdeaSubmission } from '../../../models/idea-submission';
import { CommandResult } from '../../../models/command';

@Component({
  selector: 'app-idea-input',
  templateUrl: './idea-input.component.html',
  styleUrls: ['./idea-input.component.scss']
})
export class IdeaInputComponent implements OnInit {
  ideaForm: FormGroup;
  isSubmitting = false;
  submissionSuccess = false;
  submissionError: string | null = null;
  selectedFile: File | null = null;

  constructor(
    private fb: FormBuilder,
    private ideaService: IdeaService,
    private router: Router
  ) {
    this.ideaForm = this.fb.group({
      projectName: ['', Validators.required],
      text: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  ngOnInit(): void {
  }

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      // Validate file size (10MB limit)
      if (file.size > 10 * 1024 * 1024) {
        this.submissionError = 'File size exceeds 10MB limit';
        this.selectedFile = null;
        return;
      }
      this.submissionError = null;
    }
  }

  onSubmit(): void {
    if (this.ideaForm.valid && !this.isSubmitting) {
      this.isSubmitting = true;
      this.submissionError = null;
      
      const submission: IdeaSubmission = {
        projectName: this.ideaForm.value.projectName,
        text: this.ideaForm.value.text,
        file: this.selectedFile
      };

      this.ideaService.submitIdea(submission).subscribe({
        next: (result: CommandResult<void>) => {
          this.isSubmitting = false;
          if (result.success) {
            this.submissionSuccess = true;
            // Redirect to a success page or back to project view
            setTimeout(() => {
              this.router.navigate(['/projects']);
            }, 2000);
          } else {
            this.submissionError = result.error || 'Failed to submit idea';
          }
        },
        error: (error) => {
          this.isSubmitting = false;
          this.submissionError = 'Network error occurred. Please try again.';
        }
      });
    }
  }

  onCancel(): void {
    this.router.navigate(['/projects']);
  }

  get projectName() {
    return this.ideaForm.get('projectName');
  }

  get text() {
    return this.ideaForm.get('text');
  }
}
