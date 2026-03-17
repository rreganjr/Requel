import { EntityReferenceDto } from './entity-reference';

export interface StoryDto {
  id: number;
  version: number;
  name: string;
  text: string;
  storyType: 'Success' | 'Exception';
  createdBy: string | null;
  goals: EntityReferenceDto[] | null;
  actors: EntityReferenceDto[] | null;
}
