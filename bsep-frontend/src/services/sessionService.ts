import api from '../api/api';
import { UserSession } from '../types/session';

class SessionService {
  async getActiveSessions(): Promise<UserSession[]> {
    const response = await api.get('/sessions/active');
    return response.data;
  }

  async revokeSession(sessionId: string): Promise<void> {
    await api.delete(`/sessions/${sessionId}`);
  }

  async logout(): Promise<void> {
    await api.post('/auth/logout');
  }
}

export const sessionService = new SessionService();
