export interface ProjectDto {
  name: string;
  description: string | null;
  organizationName: string | null;
  createdBy: string | null;
  status: string | null;
  stakeholderCount: number;
  goalCount: number;
  storyCount: number;
  actorCount: number;
  useCaseCount: number;
  scenarioCount: number;
  glossaryTermCount: number;
}
