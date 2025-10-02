export interface Certificate {
  id: number;
  serialNumber: string;
  commonName: string;
  organization: string;
  type: CertificateType;
  isCa: boolean;
  notBefore: string;
  notAfter: string;
  createdAt: string;
  ownerId?: number;
  ownerEmail?: string;
  ownerName?: string;
  issuerSerialNumber?: string;
  issuerCommonName?: string;
  certificateData?: string;
}

export enum CertificateType {
  ROOT_CA = 'ROOT_CA',
  INTERMEDIATE_CA = 'INTERMEDIATE_CA',
  END_ENTITY = 'END_ENTITY'
}

export interface CreateCertificateRequest {
  commonName: string;
  organization: string;
  country: string;
  certificateType: CertificateType;
  validityDays: number;
  parentCaSerialNumber: string;
  subjectAlternativeNames?: string[];
}

export interface HttpsConfigurationResponse {
  applicationProperties: string;
  keystoreFile: Uint8Array;
  keystoreFilename: string;
  certificateInfo: string;
  subjectAlternativeNames: string[];
  commonName: string;
  validityPeriod: string;
}