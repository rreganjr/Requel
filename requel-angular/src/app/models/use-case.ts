import { GoalDto } from './goal';
import { ActorDto } from './actor';
import { StoryDto } from './story';
import { ScenarioDto } from './scenario';

export interface UseCaseDto {
  id: number;
  version: number;
  name: string;
  text: string | null;
  primaryActorName: string | null;
  createdBy: string | null;
  scenarioId: number | null;
  scenarioName: string | null;
  scenarioStepCount: number | null;
  goals: GoalDto[] | null;
  actors: ActorDto[] | null;
  stories: StoryDto[] | null;
  additionalScenarios: ScenarioDto[] | null;
}

export interface EditUseCaseInput {
  projectName: string;
  useCaseId: number | null;
  name: string;
  text: string | null;
  primaryActorName: string | null;
  version: number | null;
}
