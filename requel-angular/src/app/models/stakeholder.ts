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
