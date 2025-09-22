import api from '../api/api';
import { AuthData, LoginRequest, UserDto } from '../types/user';

class AuthService {
  async login(loginRequest: LoginRequest): Promise<AuthData> {
    const response = await api.post('/auth/login', loginRequest);
    return response.data;
  }

  async register(userDto: UserDto): Promise<UserDto> {
    const response = await api.post('/auth/register', userDto);
    return response.data;
  }

  async activateAccount(token: string): Promise<string> {
    const response = await api.get(`/auth/activate/${token}`);
    return response.data;
  }

  // Token management
  setToken(token: string): void {
    localStorage.setItem('token', token);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  removeToken(): void {
    localStorage.removeItem('token');
  }

  // User data management
  setUser(user: UserDto): void {
    localStorage.setItem('user', JSON.stringify(user));
  }

  getUser(): UserDto | null {
    const userData = localStorage.getItem('user');
    return userData ? JSON.parse(userData) : null;
  }

  removeUser(): void {
    localStorage.removeItem('user');
  }

  // Check if user is authenticated
  isAuthenticated(): boolean {
    const token = this.getToken();
    const user = this.getUser();
    return !!(token && user);
  }

  // Logout - clear all auth data
  logout(): void {
    this.removeToken();
    this.removeUser();
  }
}

export const authService = new AuthService();