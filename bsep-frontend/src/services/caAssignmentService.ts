import api from '../api/api';
import { CAAssignmentRequest, CAAssignmentResponse } from '../types/caAssignment';

class CAAssignmentService {
  async assignCAToCaUser(request: CAAssignmentRequest): Promise<CAAssignmentResponse> {
    const response = await api.post('/ca-assignments', request);
    return response.data;
  }

  async revokeCAAssignment(assignmentId: number): Promise<string> {
    const response = await api.delete(`/ca-assignments/${assignmentId}`);
    return response.data;
  }

  async getAllAssignments(): Promise<CAAssignmentResponse[]> {
    const response = await api.get('/ca-assignments');
    return response.data;
  }

  async getAssignmentsByOrganization(organization: string): Promise<CAAssignmentResponse[]> {
    const response = await api.get(`/ca-assignments/organization/${organization}`);
    return response.data;
  }

  async getMyAssignments(): Promise<CAAssignmentResponse[]> {
    const response = await api.get('/ca-assignments/my');
    return response.data;
  }
}

export const caAssignmentService = new CAAssignmentService();
