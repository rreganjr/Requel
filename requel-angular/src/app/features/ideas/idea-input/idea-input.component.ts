import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { IdeaService } from '../idea.service';

@Component({
  selector: 'app-idea-input',
  templateUrl: './idea-input.component.html',
  styleUrls: ['./idea-input.component.scss'],
  imports: [ReactiveFormsModule]
})
export class IdeaInputComponent {
  ideaForm: FormGroup;
  isSubmitting = false;
  submissionError: string | null = null;
  selectedFile: File | null = null;
  extractedText: string | null = null;
  // For now, we'll assume user is PM - in a real app this would be checked against auth service
  userIsPM = true;

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

  onFileSelected(event: any): void {
    const file: File | undefined = event.target.files?.[0];
    if (!file) {
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      this.submissionError = 'File size exceeds 10MB limit';
      this.selectedFile = null;
      event.target.value = '';
      return;
    }
    
    this.selectedFile = file;
    this.submissionError = null;
    
    // Extract text from file if it's a supported text file
    if (file.type.startsWith('text/') || file.name.match(/\.(txt|md)$/i)) {
      const reader = new FileReader();
      reader.onload = (e) => {
        const text = e.target?.result as string;
        this.extractedText = text;
        // Auto-fill the text area with extracted text
        if (this.ideaForm.value.text === '') {
          this.ideaForm.patchValue({ text: text });
        }
      };
      reader.readAsText(file);
    } else {
      this.extractedText = null;
    }
  }

  async onSubmit(): Promise<void> {
    if (!this.userIsPM) {
      this.submissionError = 'Only Project Managers can submit ideas';
      return;
    }
    
    if (this.ideaForm.invalid || this.isSubmitting) {
      this.ideaForm.markAllAsTouched();
      return;
    }
    
    // Validate that either text or file is provided
    if (!this.ideaForm.value.text && !this.selectedFile) {
      this.submissionError = 'Either text description or a file must be provided';
      return;
    }
    
    // Validate minimum length for text
    if (this.ideaForm.value.text && this.ideaForm.value.text.length < 10) {
      this.submissionError = 'Description must be at least 10 characters';
      return;
    }
    
    this.isSubmitting = true;
    this.submissionError = null;

    try {
      // A .txt/.md file was read into the description box, so the text is all that is sent.
      const started = await this.ideaService.startNewProject(
        this.ideaForm.value.projectName.trim(), this.ideaForm.value.text);
      await this.router.navigate(['/ideas', started.projectName, started.ideaId]);
    } catch (err) {
      this.submissionError = err instanceof Error ? err.message : 'Failed to submit idea';
    } finally {
      this.isSubmitting = false;
    }
  }

  onCancel(): void {
    this.router.navigate(['/']);
  }

  get projectName() {
    return this.ideaForm.get('projectName');
  }

  get text() {
    return this.ideaForm.get('text');
  }
}
