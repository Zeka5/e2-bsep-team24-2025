import React, { useState, useEffect } from 'react';
import { CAAssignmentRequest, CAAssignmentResponse } from '../types/caAssignment';
import { caAssignmentService } from '../services/caAssignmentService';
import { userService } from '../services/userService';
import { certificateService } from '../services/certificateService';
import { UserDto } from '../types/user';
import { Certificate } from '../types/certificate';

const CAAssignmentManagement: React.FC = () => {
  const [assignments, setAssignments] = useState<CAAssignmentResponse[]>([]);
  const [caUsers, setCAUsers] = useState<UserDto[]>([]);
  const [caCertificates, setCACertificates] = useState<Certificate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showAssignForm, setShowAssignForm] = useState(false);

  const [assignForm, setAssignForm] = useState<CAAssignmentRequest>({
    caUserId: 0,
    caCertificateSerialNumber: ''
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [assignmentsData, usersData, certsData] = await Promise.all([
        caAssignmentService.getAllAssignments(),
        userService.getAllUsers(),
        certificateService.getAllCertificates()
      ]);

      setAssignments(assignmentsData);
      // Filter only CA users
      setCAUsers(usersData.filter(user => user.role === 'CA'));
      // Filter only CA certificates
      setCACertificates(certsData.filter(cert => cert.isCa));
    } catch (err: any) {
      setError('Failed to load data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleAssignCA = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    try {
      await caAssignmentService.assignCAToCaUser(assignForm);
      setSuccess('CA certificate assigned successfully!');
      setAssignForm({
        caUserId: 0,
        caCertificateSerialNumber: ''
      });
      setShowAssignForm(false);
      loadData();
    } catch (err: any) {
      setError(err.response?.data || 'Failed to assign CA certificate');
      console.error(err);
    }
  };

  const handleRevokeAssignment = async (assignmentId: number, caUserName: string, certName: string) => {
    if (!window.confirm(`Are you sure you want to revoke CA assignment for ${caUserName} (${certName})?`)) {
      return;
    }

    try {
      await caAssignmentService.revokeCAAssignment(assignmentId);
      setSuccess('CA assignment revoked successfully');
      loadData();
    } catch (err: any) {
      setError(err.response?.data || 'Failed to revoke CA assignment');
      console.error(err);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-900 flex items-center justify-center">
        <div className="text-white text-xl">Loading CA assignments...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900 p-6">
      <div className="max-w-7xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold text-white">CA Assignment Management</h1>
          <button
            onClick={() => setShowAssignForm(true)}
            className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded"
          >
            Assign CA Certificate
          </button>
        </div>

        {error && (
          <div className="bg-red-600 text-white p-4 rounded mb-6">
            {error}
            <button onClick={() => setError(null)} className="ml-4 text-red-200 hover:text-white">×</button>
          </div>
        )}

        {success && (
          <div className="bg-green-600 text-white p-4 rounded mb-6">
            {success}
            <button onClick={() => setSuccess(null)} className="ml-4 text-green-200 hover:text-white">×</button>
          </div>
        )}

        {/* CA Assignment Form */}
        {showAssignForm && (
          <div className="bg-gray-800 p-6 rounded mb-6">
            <h2 className="text-xl font-bold text-white mb-4">Assign CA Certificate to CA User</h2>
            <form onSubmit={handleAssignCA} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-white mb-2">CA User</label>
                  <select
                    value={assignForm.caUserId}
                    onChange={(e) => setAssignForm({ ...assignForm, caUserId: parseInt(e.target.value) })}
                    className="w-full p-2 bg-gray-700 text-white rounded"
                    required
                  >
                    <option value={0}>Select CA User</option>
                    {caUsers.map(user => (
                      <option key={user.id} value={user.id}>
                        {user.name} {user.surname} - {user.organization} ({user.email})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-white mb-2">CA Certificate</label>
                  <select
                    value={assignForm.caCertificateSerialNumber}
                    onChange={(e) => setAssignForm({ ...assignForm, caCertificateSerialNumber: e.target.value })}
                    className="w-full p-2 bg-gray-700 text-white rounded"
                    required
                  >
                    <option value="">Select CA Certificate</option>
                    {caCertificates.map(cert => (
                      <option key={cert.serialNumber} value={cert.serialNumber}>
                        {cert.commonName} ({cert.type}) - SN: {cert.serialNumber}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="text-sm text-gray-400">
                This will grant the CA user permission to use this CA certificate for signing new certificates.
              </div>
              <div className="flex space-x-4">
                <button type="submit" className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded">
                  Assign CA Certificate
                </button>
                <button
                  type="button"
                  onClick={() => setShowAssignForm(false)}
                  className="bg-gray-600 hover:bg-gray-700 text-white px-4 py-2 rounded"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {/* CA Assignments List */}
        <div className="bg-gray-800 rounded overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-700">
              <tr>
                <th className="text-left p-4 text-white">CA User</th>
                <th className="text-left p-4 text-white">Email</th>
                <th className="text-left p-4 text-white">Organization</th>
                <th className="text-left p-4 text-white">CA Certificate</th>
                <th className="text-left p-4 text-white">Serial Number</th>
                <th className="text-left p-4 text-white">Assigned At</th>
                <th className="text-left p-4 text-white">Assigned By</th>
                <th className="text-left p-4 text-white">Status</th>
                <th className="text-left p-4 text-white">Actions</th>
              </tr>
            </thead>
            <tbody>
              {assignments.map((assignment) => (
                <tr key={assignment.id} className="border-t border-gray-700">
                  <td className="p-4 text-white">{assignment.caUserName}</td>
                  <td className="p-4 text-gray-300">{assignment.caUserEmail}</td>
                  <td className="p-4 text-gray-300">{assignment.organization}</td>
                  <td className="p-4 text-white">{assignment.caCertificateCommonName}</td>
                  <td className="p-4 text-gray-300 font-mono text-sm">
                    {assignment.caCertificateSerialNumber}
                  </td>
                  <td className="p-4 text-gray-300">
                    {new Date(assignment.assignedAt).toLocaleString()}
                  </td>
                  <td className="p-4 text-gray-300">{assignment.assignedByEmail}</td>
                  <td className="p-4">
                    <span className={`px-2 py-1 rounded text-xs ${assignment.active ? 'bg-green-600' : 'bg-gray-600'
                      }`}>
                      {assignment.active ? 'Active' : 'Revoked'}
                    </span>
                  </td>
                  <td className="p-4">
                    {assignment.active ? (
                      <button
                        onClick={() => handleRevokeAssignment(
                          assignment.id,
                          assignment.caUserName,
                          assignment.caCertificateCommonName
                        )}
                        className="bg-red-500 hover:bg-red-600 text-white px-3 py-1 rounded text-sm"
                      >
                        Revoke
                      </button>
                    ) : (
                      <span className="text-gray-500 text-sm">Revoked</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {assignments.length === 0 && (
            <div className="p-8 text-center text-gray-400">
              No CA assignments found. Assign a CA certificate to a CA user to get started.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default CAAssignmentManagement;
