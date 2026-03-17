export interface StakeholderDto {
  id: number;
  version: number;
  name: string;
  type: 'user' | 'non-user';
  createdBy: string | null;
  userDetails: UserStakeholderDetails | null;
  nonUserDetails: NonUserStakeholderDetails | null;
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
