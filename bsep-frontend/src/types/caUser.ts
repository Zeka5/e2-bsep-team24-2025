export interface CreateCAUserRequest {
  name: string;
  surname: string;
  email: string;
  organization: string;
}

export interface CAUserResponse extends UserDto {
  temporaryPassword?: string;
}

import { UserDto } from './user';
