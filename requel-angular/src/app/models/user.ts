export interface UserDto {
  id: number;
  username: string;
  name: string;
  emailAddress: string | null;
  phoneNumber: string | null;
  organizationName: string | null;
  roles: string[];
  permissions: string[];
  version: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: UserDto;
}
