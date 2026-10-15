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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { DictionaryWordDto, IgnoredFindingDto } from '../models/dictionary';
import { CommandService } from './command.service';

/**
 * The spell-check word lists users manage (issue #319): a project's own dictionary, gated on
 * Project[Edit], and the installation-wide dictionary, for administrators. Reads are plain GETs;
 * writes go through the CQRS command endpoint.
 */
@Injectable({ providedIn: 'root' })
export class DictionaryService {
  constructor(private http: HttpClient, private commandService: CommandService) {}

  listProjectWords(projectName: string): Promise<DictionaryWordDto[]> {
    return firstValueFrom(this.http.get<DictionaryWordDto[]>(
      `${environment.apiBaseUrl}/projects/${encodeURIComponent(projectName)}/dictionary`));
  }

  addProjectWord(projectName: string, lemma: string) {
    return this.commandService.execute<DictionaryWordDto>('AddProjectDictionaryWord',
      { projectName, lemma });
  }

  removeProjectWord(projectName: string, wordId: number) {
    return this.commandService.execute('DeleteProjectDictionaryWord', { projectName, wordId });
  }

  /** The assistant findings ignored in the project (issue #320). */
  listIgnoredFindings(projectName: string): Promise<IgnoredFindingDto[]> {
    return firstValueFrom(this.http.get<IgnoredFindingDto[]>(
      `${environment.apiBaseUrl}/projects/${encodeURIComponent(projectName)}/ignored-findings`));
  }

  /** Stop ignoring a finding; the server re-analyzes the entity so it is raised again. */
  removeIgnoredFinding(projectName: string, ignoredFindingId: number) {
    return this.commandService.execute('DeleteIgnoredFinding', { projectName, ignoredFindingId });
  }

  listInstallWords(): Promise<DictionaryWordDto[]> {
    return firstValueFrom(this.http.get<DictionaryWordDto[]>(
      `${environment.apiBaseUrl}/admin/dictionary`));
  }

  addInstallWord(lemma: string) {
    return this.commandService.execute<DictionaryWordDto>('AddInstallDictionaryWord', { lemma });
  }

  removeInstallWord(wordId: number) {
    return this.commandService.execute('DeleteInstallDictionaryWord', { wordId });
  }
}
