export interface PasswordEntry {
  id: number;
  website: string;
  username: string;
  encryptedPassword: string;
  certificateSerialNumber: string;
  certificateCommonName: string;
  createdAt: string;
}

export interface CreatePasswordEntryRequest {
  website: string;
  username: string;
  password: string;
  certificateSerialNumber: string;
}

export interface DecryptedPasswordEntry {
  id: number;
  website: string;
  username: string;
  decryptedPassword: string;
}
