import { EntityReferenceDto } from './entity-reference';

export interface StakeholderDto {
  id: number;
  version: number;
  name: string;
  type: 'user' | 'non-user';
  createdBy: string | null;
  userDetails: UserStakeholderDetails | null;
  nonUserDetails: NonUserStakeholderDetails | null;
  goals?: EntityReferenceDto[];
}

export interface UserStakeholderDetails {
  username: string;
  emailAddress: string;
  phoneNumber: string;
  teamName: string | null;
  permissionKeys: string[];
}

export interface NonUserStakeholderDetails {
  text: string;
}

export interface StakeholderPermissionDto {
  permissionKey: string;
  entityType: string;
  permissionType: string;
}
