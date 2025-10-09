import React, { useState, useEffect } from 'react';
import { CSRResponse, CSRStatus, UploadCSRRequest } from '../types/csr';
import { csrService } from '../services/csrService';

const MyCSRs: React.FC = () => {
  const [csrs, setCSRs] = useState<CSRResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);

  const [uploadForm, setUploadForm] = useState<UploadCSRRequest>({
    validityDays: 365,
    csrFile: null as any
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const csrsData = await csrService.getMyCSRs();
      setCSRs(csrsData);
    } catch (err: any) {
      setError('Failed to load data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleUploadCSR = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!uploadForm.csrFile) {
      setError('Please select a CSR file');
      return;
    }

    if (!uploadForm.csrFile.name.endsWith('.pem') &&
      !uploadForm.csrFile.name.endsWith('.csr')) {
      setError('Please select a .pem or .csr file');
      return;
    }

    try {
      await csrService.uploadCSR(uploadForm);
      setSuccess('CSR uploaded successfully! Waiting for CA approval.');
      setUploadForm({
        validityDays: 365,
        csrFile: null as any
      });
      setShowCreateForm(false);
      loadData();
    } catch (err: any) {
      setError(err.response?.data || 'Failed to create CSR');
      console.error(err);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setUploadForm({ ...uploadForm, csrFile: file });
    }
  };

  const getStatusBadge = (status: CSRStatus) => {
    switch (status) {
      case CSRStatus.PENDING:
        return <span className="px-2 py-1 rounded text-xs bg-yellow-600">Pending</span>;
      case CSRStatus.APPROVED:
        return <span className="px-2 py-1 rounded text-xs bg-green-600">Approved</span>;
      case CSRStatus.REJECTED:
        return <span className="px-2 py-1 rounded text-xs bg-red-600">Rejected</span>;
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-900 flex items-center justify-center">
        <div className="text-white text-xl">Loading CSRs...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900 p-6">
      <div className="max-w-7xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold text-white">My Certificate Signing Requests</h1>
          <button
            onClick={() => setShowCreateForm(true)}
            className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded"
          >
            Upload New CSR
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

        {/* CSR Upload Form */}
        {showCreateForm && (
          <div className="bg-gray-800 p-6 rounded mb-6">
            <h2 className="text-xl font-bold text-white mb-4">Upload Certificate Signing Request</h2>
            <form onSubmit={handleUploadCSR} className="space-y-4">
              <div className="space-y-4">
                <div>
                  <label className="block text-white mb-2">CSR File (.pem or .csr)</label>
                  <input
                    type="file"
                    accept=".pem,.csr"
                    onChange={handleFileChange}
                    className="w-full p-2 bg-gray-700 text-white rounded file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-sm file:font-semibold file:bg-green-600 file:text-white hover:file:bg-green-700"
                    required
                  />
                  <p className="text-gray-400 text-sm mt-1">
                    Select your Certificate Signing Request file in PEM format
                  </p>
                </div>

                <div>
                  <label className="block text-white mb-2">Validity (Days)</label>
                  <input
                    type="number"
                    value={uploadForm.validityDays}
                    onChange={(e) => setUploadForm({ ...uploadForm, validityDays: parseInt(e.target.value) })}
                    className="w-full p-2 bg-gray-700 text-white rounded"
                    min="1"
                    required
                  />
                </div>
              </div>

              <div className="flex space-x-4">
                <button
                  type="submit"
                  className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded"
                  disabled={!uploadForm.csrFile}
                >
                  Upload CSR
                </button>
                <button
                  type="button"
                  onClick={() => setShowCreateForm(false)}
                  className="bg-gray-600 hover:bg-gray-700 text-white px-4 py-2 rounded"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {/* CSRs List */}
        <div className="bg-gray-800 rounded overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-700">
              <tr>
                <th className="text-left p-4 text-white">Common Name</th>
                <th className="text-left p-4 text-white">Organization</th>
                <th className="text-left p-4 text-white">Validity</th>
                <th className="text-left p-4 text-white">Status</th>
                <th className="text-left p-4 text-white">Submitted</th>
                <th className="text-left p-4 text-white">Details</th>
              </tr>
            </thead>
            <tbody>
              {csrs.map((csr) => (
                <tr key={csr.id} className="border-t border-gray-700">
                  <td className="p-4 text-white">{csr.commonName}</td>
                  <td className="p-4 text-gray-300">{csr.organization}</td>
                  <td className="p-4 text-gray-300">{csr.validityDays} days</td>
                  <td className="p-4">{getStatusBadge(csr.status)}</td>
                  <td className="p-4 text-gray-300">
                    {new Date(csr.createdAt).toLocaleDateString()}
                  </td>
                  <td className="p-4 text-gray-300">
                    {csr.status === CSRStatus.APPROVED && csr.issuedCertificateSerialNumber && (
                      <span className="text-green-400 text-sm">
                        Certificate issued: {csr.issuedCertificateSerialNumber}
                      </span>
                    )}
                    {csr.status === CSRStatus.REJECTED && csr.rejectionReason && (
                      <span className="text-red-400 text-sm">
                        Rejected: {csr.rejectionReason}
                      </span>
                    )}
                    {csr.status === CSRStatus.PENDING && (
                      <span className="text-yellow-400 text-sm">Awaiting review</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {csrs.length === 0 && (
            <div className="p-8 text-center text-gray-400">
              No CSRs found. Submit a new CSR to request a certificate.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default MyCSRs;
