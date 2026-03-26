import { EntityReferenceDto } from './entity-reference';

export interface ActorDto {
  id: number;
  version: number;
  name: string;
  text: string | null;
  createdBy: string | null;
  goals: EntityReferenceDto[] | null;
  referencedByUseCases: EntityReferenceDto[] | null;
  referencedByStories: EntityReferenceDto[] | null;
}
