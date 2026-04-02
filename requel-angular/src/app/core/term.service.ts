import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { GlossaryTermDto } from '../models/term';
import { CommandService } from './command.service';

@Injectable({ providedIn: 'root' })
export class TermService {
  private readonly base = '/api/projects';

  constructor(private http: HttpClient, private commandService: CommandService) {}

  listTerms(projectName: string): Promise<GlossaryTermDto[]> {
    return firstValueFrom(this.http.get<GlossaryTermDto[]>(`${this.base}/${projectName}/terms`));
  }

  getTerm(projectName: string, termId: number): Promise<GlossaryTermDto> {
    return firstValueFrom(this.http.get<GlossaryTermDto>(`${this.base}/${projectName}/terms/${termId}`));
  }

  saveTerm(projectName: string, termId: number | null, name: string, text: string | null, canonicalTermId: number | null) {
    return this.commandService.execute('EditGlossaryTerm', { projectName, termId, name, text, canonicalTermId });
  }

  deleteTerm(projectName: string, termId: number) {
    return this.commandService.execute('DeleteGlossaryTerm', { projectName, termId });
  }
}
