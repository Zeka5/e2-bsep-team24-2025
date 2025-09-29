import api from '../api/api';
import { Certificate, CreateCertificateRequest, HttpsConfigurationResponse } from '../types/certificate';

export const certificateService = {
  // Get all certificates
  getAllCertificates: async (): Promise<Certificate[]> => {
    const response = await api.get('/certificates');
    return response.data;
  },

  // Create root certificate
  createRootCertificate: async (commonName: string): Promise<Certificate> => {
    const response = await api.post('/certificates/root', { commonName });
    return response.data;
  },

  // Sign a new certificate
  signCertificate: async (request: CreateCertificateRequest): Promise<Certificate> => {
    const response = await api.post('/certificates/sign', request);
    return response.data;
  },

  // Export certificate in different formats
  exportCertificate: async (serialNumber: string, format: string = 'pem'): Promise<Blob> => {
    const response = await api.get(`/certificates/${serialNumber}/export`, {
      params: { format },
      responseType: 'blob'
    });
    return response.data;
  },

  // Export keystore (PKCS12)
  exportKeystore: async (serialNumber: string, password: string): Promise<Blob> => {
    const response = await api.get(`/certificates/${serialNumber}/keystore`, {
      params: { password },
      responseType: 'blob'
    });
    return response.data;
  },

  // Get HTTPS configuration
  getHttpsConfiguration: async (serialNumber: string, keystorePassword: string): Promise<HttpsConfigurationResponse> => {
    const response = await api.get(`/certificates/${serialNumber}/https-config`, {
      params: { keystorePassword }
    });
    return response.data;
  },

  // Download application.properties
  downloadApplicationProperties: async (serialNumber: string, keystorePassword: string, port: number = 8443): Promise<Blob> => {
    const response = await api.get(`/certificates/${serialNumber}/application-properties`, {
      params: { keystorePassword, port },
      responseType: 'blob'
    });
    return response.data;
  }
};

// Helper function to download blob as file
export const downloadFile = (blob: Blob, filename: string) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
};