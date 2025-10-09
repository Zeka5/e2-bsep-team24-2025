export interface SharedPasswordEntry {
  id: number;
  website: string;
  username: string;
  encryptedPassword: string;
  sharedByUserEmail: string;
  sharedByUserName: string;
  sharedWithCertificateSerialNumber: string;
  sharedWithCertificateCommonName: string;
  createdAt: string;
}

export interface SharePasswordRequest {
  passwordEntryId: number;
  sharedWithUserId: number;
  sharedWithCertificateSerialNumber: string;
  decryptedPassword: string;
}
