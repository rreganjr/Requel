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
import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { ArtifactType } from '../route-data';
import { GoalService } from '../goal.service';
import { StoryService } from '../story.service';
import { ActorService } from '../actor.service';
import { ScenarioService } from '../scenario.service';
import { UseCaseService } from '../use-case.service';
import { TermService } from '../term.service';
import { ReportService } from '../report.service';
import { StakeholderService } from '../stakeholder.service';

/**
 * Resolves an artifact editor route's entity to its display name, so the
 * breadcrumb leaf and the document title can show the name (e.g. "Login flow")
 * instead of the bare type ("Goal"). Keyed on the route's `artifactType`
 * (#154, moved from #128).
 *
 * Fails soft: any missing param, non-numeric id, unknown type, or fetch error
 * resolves to `null` so navigation is never blocked and the breadcrumb falls
 * back to its static type label. Only attached to the artifact *editor* routes
 * (those with an id param); the project editor's name is already the `:name`
 * URL segment, so it needs no resolver.
 */
export const artifactNameResolver: ResolveFn<string | null> = (route) => {
  const type = route.data['artifactType'] as ArtifactType | undefined;
  const projectName = route.paramMap.get('name');
  if (!type || !projectName) return null;

  // The id param is the one that isn't the project `:name` (goalId, storyId, …).
  const idKey = route.paramMap.keys.find(k => k !== 'name');
  const id = idKey ? Number(route.paramMap.get(idKey)) : Number.NaN;
  if (!Number.isFinite(id)) return null;

  const nameOf = (p: Promise<{ name?: string } | null>): Promise<string | null> =>
    p.then(dto => dto?.name ?? null).catch(() => null);

  switch (type) {
    case 'goal': return nameOf(inject(GoalService).getGoal(projectName, id));
    case 'story': return nameOf(inject(StoryService).getStory(projectName, id));
    case 'actor': return nameOf(inject(ActorService).getActor(projectName, id));
    case 'scenario': return nameOf(inject(ScenarioService).getScenario(projectName, id));
    case 'use-case': return nameOf(inject(UseCaseService).getUseCase(projectName, id));
    case 'term': return nameOf(inject(TermService).getTerm(projectName, id));
    case 'report': return nameOf(inject(ReportService).getReport(projectName, id));
    case 'stakeholder': return nameOf(inject(StakeholderService).getStakeholder(projectName, id));
    default: return null;
  }
};
