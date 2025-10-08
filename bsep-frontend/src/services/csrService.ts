import api from '../api/api';
import { CreateCSRRequest, CSRResponse, ReviewCSRRequest, CSRStatus, UploadCSRRequest } from '../types/csr';

class CSRService {
  async createCSR(request: CreateCSRRequest): Promise<CSRResponse> {
    const response = await api.post('/csr', request);
    return response.data;
  }

  async uploadCSR(request: UploadCSRRequest): Promise<CSRResponse> {
    const formData = new FormData();
    formData.append('csrFile', request.csrFile);
    formData.append('validityDays', request.validityDays.toString());

    const response = await api.post('/csr/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  }

  async getMyCSRs(): Promise<CSRResponse[]> {
    const response = await api.get('/csr/my');
    return response.data;
  }

  async getAllCSRs(): Promise<CSRResponse[]> {
    const response = await api.get('/csr');
    return response.data;
  }

  async getCSRsByStatus(status: CSRStatus): Promise<CSRResponse[]> {
    const response = await api.get(`/csr/status/${status}`);
    return response.data;
  }

  async getCSRById(id: number): Promise<CSRResponse> {
    const response = await api.get(`/csr/${id}`);
    return response.data;
  }

  async reviewCSR(id: number, request: ReviewCSRRequest): Promise<CSRResponse> {
    const response = await api.post(`/csr/${id}/review`, request);
    return response.data;
  }
}

export const csrService = new CSRService();
