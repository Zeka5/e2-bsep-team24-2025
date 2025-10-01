import api from '../api/api';
import { UserDto } from '../types/user';
import { CreateCAUserRequest } from '../types/caUser';

class UserService {
  async getAllUsers(): Promise<UserDto[]> {
    const response = await api.get('/users/get-all');
    return response.data;
  }

  async getMyInfo(): Promise<UserDto> {
    const response = await api.get('/users/me');
    return response.data;
  }

  async updateMyInfo(userDto: UserDto): Promise<UserDto> {
    const response = await api.put('/users/me', userDto);
    return response.data;
  }

  async createCAUser(request: CreateCAUserRequest): Promise<UserDto> {
    const response = await api.post('/users/ca-users', request);
    return response.data;
  }

  async deleteUser(id: number): Promise<string> {
    const response = await api.delete(`/users/${id}`);
    return response.data;
  }
}

export const userService = new UserService();
