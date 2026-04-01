import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { AnnotationsDto } from '../models/annotation';
import { CommandService } from './command.service';

@Injectable({ providedIn: 'root' })
export class AnnotationService {
  private readonly base = '/api/annotations';

  constructor(private http: HttpClient, private commandService: CommandService) {}

  getAnnotations(projectName: string, entityType: string, entityId: number): Promise<AnnotationsDto> {
    const params = new HttpParams()
      .set('projectName', projectName)
      .set('entityType', entityType)
      .set('entityId', entityId.toString());
    return firstValueFrom(this.http.get<AnnotationsDto>(this.base, { params }));
  }

  addNote(projectName: string, entityType: string, entityId: number, text: string) {
    return this.commandService.execute('EditNote', { projectName, entityType, entityId, text });
  }

  deleteNote(projectName: string, noteId: number) {
    return this.commandService.execute('DeleteNote', { projectName, noteId });
  }

  addIssue(projectName: string, entityType: string, entityId: number, text: string, mustBeResolved: boolean) {
    return this.commandService.execute('EditIssue', { projectName, entityType, entityId, text, mustBeResolved });
  }

  deleteIssue(projectName: string, issueId: number) {
    return this.commandService.execute('DeleteIssue', { projectName, issueId });
  }

  addPosition(projectName: string, issueId: number, text: string) {
    return this.commandService.execute('EditPosition', { projectName, issueId, text });
  }

  deletePosition(projectName: string, positionId: number) {
    return this.commandService.execute('DeletePosition', { projectName, positionId });
  }

  addArgument(projectName: string, positionId: number, text: string, supportLevel: string) {
    return this.commandService.execute('EditArgument', { projectName, positionId, text, supportLevel });
  }

  deleteArgument(projectName: string, argumentId: number) {
    return this.commandService.execute('DeleteArgument', { projectName, argumentId });
  }

  resolveIssue(projectName: string, issueId: number, positionId: number) {
    return this.commandService.execute('ResolveIssue', { projectName, issueId, positionId });
  }
}
