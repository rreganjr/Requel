import { EntityReferenceDto } from './entity-reference';

export interface GoalDto {
  id: number;
  version: number;
  name: string;
  text: string;
  createdBy: string | null;
  relationsFromThisGoal: GoalRelationDto[] | null;
  relationsToThisGoal: GoalRelationDto[] | null;
  referencedBy: EntityReferenceDto[] | null;
}

export interface GoalRelationDto {
  id: number;
  version: number;
  goalId: number;
  goalName: string;
  relationType: 'Supports' | 'Conflicts';
}
