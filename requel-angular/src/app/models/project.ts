export interface ProjectDto {
  id: number;
  version: number;
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
  reportGeneratorCount: number;
}

export interface ProjectPermissions {
  isStakeholder: boolean;
  canCreateProjects: boolean;
  permissions: Record<string, string[]>;
}

export interface ProjectTreeNode {
  id?: number;
  type: string;
  name: string;
  children?: ProjectTreeNode[];
}
