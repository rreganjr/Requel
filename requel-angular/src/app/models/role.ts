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
export interface RoleDto {
  roleName: string;
  displayName: string;
  availablePermissions: PermissionDto[];
}

export interface PermissionDto {
  name: string;
}

// Predefined roles with their permissions
export const SYSTEM_ROLES: RoleDto[] = [
  {
    roleName: 'PRODUCT_MANAGER',
    displayName: 'Product Manager',
    availablePermissions: [
      { name: 'REVIEW_REQUIREMENT' },
      { name: 'APPROVE_REQUIREMENT' },
      { name: 'CREATE_REQUIREMENT' },
      { name: 'EDIT_REQUIREMENT' }
    ]
  },
  {
    roleName: 'ENGINEER',
    displayName: 'Engineer',
    availablePermissions: [
      { name: 'REVIEW_REQUIREMENT' },
      { name: 'IMPLEMENT_REQUIREMENT' },
      { name: 'CREATE_TECHNICAL_SPEC' }
    ]
  },
  {
    roleName: 'TEST_ENGINEER',
    displayName: 'Test Engineer',
    availablePermissions: [
      { name: 'REVIEW_REQUIREMENT' },
      { name: 'CREATE_TEST_CASE' },
      { name: 'EXECUTE_TEST' },
      { name: 'REPORT_DEFECT' }
    ]
  },
  {
    roleName: 'ANALYST',
    displayName: 'Analyst',
    availablePermissions: [
      { name: 'REVIEW_REQUIREMENT' },
      { name: 'ANALYZE_REQUIREMENT' },
      { name: 'CREATE_USE_CASE' },
      { name: 'TRACE_REQUIREMENT' }
    ]
  }
];

// Permission constants for frontend authorization checks
export const PERMISSIONS = {
  REVIEW_REQUIREMENT: 'REVIEW_REQUIREMENT',
  APPROVE_REQUIREMENT: 'APPROVE_REQUIREMENT',
  CREATE_REQUIREMENT: 'CREATE_REQUIREMENT',
  EDIT_REQUIREMENT: 'EDIT_REQUIREMENT',
  IMPLEMENT_REQUIREMENT: 'IMPLEMENT_REQUIREMENT',
  CREATE_TECHNICAL_SPEC: 'CREATE_TECHNICAL_SPEC',
  CREATE_TEST_CASE: 'CREATE_TEST_CASE',
  EXECUTE_TEST: 'EXECUTE_TEST',
  REPORT_DEFECT: 'REPORT_DEFECT',
  ANALYZE_REQUIREMENT: 'ANALYZE_REQUIREMENT',
  CREATE_USE_CASE: 'CREATE_USE_CASE',
  TRACE_REQUIREMENT: 'TRACE_REQUIREMENT'
} as const;
