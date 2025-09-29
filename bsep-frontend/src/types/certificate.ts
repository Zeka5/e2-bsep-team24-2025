export interface Certificate {
  serialNumber: string;
  commonName: string;
  notBefore: string;
  notAfter: string;
  type: CertificateType;
  isCa: boolean;
  certificateData: string;
  owner: {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
  };
  issuer?: Certificate;
  createdAt: string;
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