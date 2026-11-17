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
export interface UserDto {
  id: number;
  username: string;
  name: string;
  emailAddress: string | null;
  phoneNumber: string | null;
  organizationName: string | null;
  roles: string[];
  permissions: string[];
  permissionsByRole: Record<string, string[]> | null;
  version: number;
  // Primary role for UI display and quick authorization checks
  primaryRole?: string;
}

export interface OrganizationDto {
  id: number;
  name: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: UserDto;
}

// Role constants for frontend use
export const USER_ROLES = {
  PRODUCT_MANAGER: 'PRODUCT_MANAGER',
  ENGINEER: 'ENGINEER',
  TEST_ENGINEER: 'TEST_ENGINEER',
  ANALYST: 'ANALYST'
} as const;

export type UserRoleType = typeof USER_ROLES[keyof typeof USER_ROLES];
