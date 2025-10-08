export interface CAAssignmentRequest {
  caUserId: number;
  caCertificateSerialNumber: string;
}

export interface CAAssignmentResponse {
  id: number;
  caUserId: number;
  caUserName: string;
  caUserEmail: string;
  organization: string;
  caCertificateSerialNumber: string;
  caCertificateCommonName: string;
  assignedAt: string;
  assignedByEmail: string;
  active: boolean;
}
