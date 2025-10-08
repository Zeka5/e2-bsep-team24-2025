export enum CSRStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED'
}

export interface CreateCSRRequest {
  commonName: string;
  organization: string;
  country: string;
  validityDays: number;
  subjectAlternativeNames?: string[];
}

export interface UploadCSRRequest {
  validityDays: number;
  csrFile: File; // the uploaded .pem file
}

export interface CSRResponse {
  id: number;
  commonName: string;
  organization: string;
  country: string;
  validityDays: number;
  status: CSRStatus;
  createdAt: string;
  reviewedAt?: string;
  rejectionReason?: string;
  requesterEmail: string;
  reviewerEmail?: string;
  selectedCaCommonName?: string;
  issuedCertificateSerialNumber?: string;
}

export interface ReviewCSRRequest {
  approved: boolean;
  rejectionReason?: string;
  selectedCaSerialNumber?: string;
}
