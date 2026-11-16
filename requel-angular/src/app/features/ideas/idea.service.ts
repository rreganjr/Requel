import { Injectable } from '@angular/core';
import { CommandService } from '../../core/command.service';
import { CommandResult } from '../../models/command';
import { IdeaSubmission } from '../../models/idea-submission';

@Injectable({ providedIn: 'root' })
export class IdeaService {
  constructor(private commandService: CommandService) {}

  submitIdea(submission: IdeaSubmission): Promise<CommandResult> {
    const input = { projectName: submission.projectName, text: submission.text };
    return submission.file
      ? this.commandService.executeWithFile('SubmitIdea', input, submission.file)
      : this.commandService.execute('SubmitIdea', input);
  }
}
