import api from '../api/api';
import { PasswordEntry, CreatePasswordEntryRequest } from '../types/passwordEntry';

export const passwordManagerService = {
  // Get all password entries for current user
  getPasswordEntries: async (): Promise<PasswordEntry[]> => {
    const response = await api.get('/password-manager/entries');
    return response.data;
  },

  // Create a new password entry
  createPasswordEntry: async (request: CreatePasswordEntryRequest): Promise<PasswordEntry> => {
    const response = await api.post('/password-manager/entries', request);
    return response.data;
  },

  // Delete a password entry
  deletePasswordEntry: async (entryId: number): Promise<void> => {
    await api.delete(`/password-manager/entries/${entryId}`);
  },
};
