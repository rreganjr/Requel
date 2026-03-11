export interface RoleDto {
  roleName: string;
  displayName: string;
  availablePermissions: PermissionDto[];
}

export interface PermissionDto {
  name: string;
}
