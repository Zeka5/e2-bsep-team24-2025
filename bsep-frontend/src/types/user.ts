export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN'
}

export interface User {
  id: number;
  email: string;
  name: string;
  surname: string;
  role: UserRole;
  isActivated: boolean;
  activationToken?: string;
  tokenExpiry?: string;
  createdAt: string;
}

export interface UserDto {
  id?: number;
  name: string;
  surname: string;
  email: string;
  password?: string;
  organization: string;
  role?: string;
  createdAt?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
  captchaToken: string;
}

export interface AuthData {
  user: UserDto;
  token: string;
  expirationDate: string;
}

export interface AuthContextType {
  user: UserDto | null;
  token: string | null;
  login: (email: string, password: string, captchaToken: string) => Promise<void>;
  register: (userData: UserDto) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  isLoading: boolean;
}