import api from '../api/api';
import { SharedPasswordEntry, SharePasswordRequest } from '../types/sharedPassword';

export const sharedPasswordService = {
  sharePassword: async (request: SharePasswordRequest): Promise<SharedPasswordEntry> => {
    const response = await api.post<SharedPasswordEntry>('/shared-passwords', request);
    return response.data;
  },

  getSharedPasswords: async (): Promise<SharedPasswordEntry[]> => {
    const response = await api.get<SharedPasswordEntry[]>('/shared-passwords');
    return response.data;
  },

  deleteSharedPassword: async (sharedEntryId: number): Promise<void> => {
    await api.delete(`/shared-passwords/${sharedEntryId}`);
  }
};
