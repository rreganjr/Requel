import { Injectable } from '@angular/core';
import { CommandService } from '../../core/command.service';
import { IdeaSubmission } from '../../models/idea-submission';
import { Observable } from 'rxjs';
import { CommandResult } from '../../models/command';

@Injectable({
  providedIn: 'root'
})
export class IdeaService {

  constructor(private commandService: CommandService) { }

  submitIdea(submission: IdeaSubmission): Observable<CommandResult<void>> {
    if (submission.file) {
      // Handle file upload
      return new Observable(observer => {
        const formData = new FormData();
        formData.append('input', new Blob([JSON.stringify({
          projectName: submission.projectName,
          text: submission.text
        })], { type: 'application/json' }));
        formData.append('file', submission.file);
        
        this.commandService.executeWithFile('SubmitIdea', {
          projectName: submission.projectName,
          text: submission.text
        }, submission.file).subscribe({
          next: (result) => observer.next(result),
          error: (error) => observer.error(error),
          complete: () => observer.complete()
        });
      });
    } else {
      // Handle text-only submission
      return new Observable(observer => {
        this.commandService.execute('SubmitIdea', {
          projectName: submission.projectName,
          text: submission.text
        }).subscribe({
          next: (result) => observer.next(result),
          error: (error) => observer.error(error),
          complete: () => observer.complete()
        });
      });
    }
  }
}
