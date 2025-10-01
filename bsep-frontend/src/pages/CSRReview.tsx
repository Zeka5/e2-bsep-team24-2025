import React, { useState, useEffect } from 'react';
import { CSRResponse, CSRStatus, ReviewCSRRequest } from '../types/csr';
import { csrService } from '../services/csrService';
import { certificateService } from '../services/certificateService';
import { Certificate } from '../types/certificate';
import { useAuth } from '../contexts/AuthContext';

const CSRReview: React.FC = () => {
  const { user } = useAuth();
  const [csrs, setCSRs] = useState<CSRResponse[]>([]);
  const [filteredCSRs, setFilteredCSRs] = useState<CSRResponse[]>([]);
  const [availableCAs, setAvailableCAs] = useState<Certificate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<CSRStatus | 'ALL'>(CSRStatus.PENDING);
  const [selectedCSR, setSelectedCSR] = useState<CSRResponse | null>(null);
  const [showReviewModal, setShowReviewModal] = useState(false);
  const [reviewForm, setReviewForm] = useState<ReviewCSRRequest>({
    approved: true,
    rejectionReason: '',
    selectedCaSerialNumber: ''
  });

  useEffect(() => {
    loadCSRs();
  }, []);

  useEffect(() => {
    filterCSRs();
  }, [statusFilter, csrs]);

  const loadCSRs = async () => {
    try {
      setLoading(true);

      // CA users get their certificates (/certificates/my)
      // Admin users get all CA certificates (/certificates/ca-certificates)
      const certificatePromise = user?.role === 'ADMIN'
        ? certificateService.getCACertificates()
        : certificateService.getMyCertificates();

      const [csrsData, casData] = await Promise.all([
        csrService.getAllCSRs(),
        certificatePromise
      ]);

      setCSRs(csrsData);
      // For CA users, filter only CA certificates from their certs
      // For Admin, getCACertificates already returns only CA certs
      const caCerts = user?.role === 'ADMIN'
        ? casData
        : casData.filter(cert => cert.isCa);
      setAvailableCAs(caCerts);
    } catch (err: any) {
      setError('Failed to load CSRs');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const filterCSRs = () => {
    if (statusFilter === 'ALL') {
      setFilteredCSRs(csrs);
    } else {
      setFilteredCSRs(csrs.filter(csr => csr.status === statusFilter));
    }
  };

  const handleReviewClick = (csr: CSRResponse) => {
    setSelectedCSR(csr);
    setReviewForm({
      approved: true,
      rejectionReason: '',
      selectedCaSerialNumber: ''
    });
    setShowReviewModal(true);
    setError(null);
  };

  const handleSubmitReview = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!selectedCSR) return;

    if (!reviewForm.approved && !reviewForm.rejectionReason) {
      setError('Rejection reason is required when rejecting a CSR');
      return;
    }

    if (reviewForm.approved && !reviewForm.selectedCaSerialNumber) {
      setError('Please select a CA certificate for signing');
      return;
    }

    try {
      await csrService.reviewCSR(selectedCSR.id, reviewForm);
      setSuccess(`CSR ${reviewForm.approved ? 'approved' : 'rejected'} successfully!`);
      setShowReviewModal(false);
      setSelectedCSR(null);
      loadCSRs();
    } catch (err: any) {
      setError(err.response?.data || 'Failed to review CSR');
      console.error(err);
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
          <h1 className="text-3xl font-bold text-white">CSR Review & Approval</h1>

          {/* Status Filter */}
          <div className="flex space-x-2">
            <button
              onClick={() => setStatusFilter('ALL')}
              className={`px-4 py-2 rounded ${statusFilter === 'ALL' ? 'bg-blue-600' : 'bg-gray-700'} text-white`}
            >
              All
            </button>
            <button
              onClick={() => setStatusFilter(CSRStatus.PENDING)}
              className={`px-4 py-2 rounded ${statusFilter === CSRStatus.PENDING ? 'bg-yellow-600' : 'bg-gray-700'} text-white`}
            >
              Pending
            </button>
            <button
              onClick={() => setStatusFilter(CSRStatus.APPROVED)}
              className={`px-4 py-2 rounded ${statusFilter === CSRStatus.APPROVED ? 'bg-green-600' : 'bg-gray-700'} text-white`}
            >
              Approved
            </button>
            <button
              onClick={() => setStatusFilter(CSRStatus.REJECTED)}
              className={`px-4 py-2 rounded ${statusFilter === CSRStatus.REJECTED ? 'bg-red-600' : 'bg-gray-700'} text-white`}
            >
              Rejected
            </button>
          </div>
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

        {/* CSRs List */}
        <div className="bg-gray-800 rounded overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-700">
              <tr>
                <th className="text-left p-4 text-white">Common Name</th>
                <th className="text-left p-4 text-white">Organization</th>
                <th className="text-left p-4 text-white">Requester</th>
                <th className="text-left p-4 text-white">Validity</th>
                <th className="text-left p-4 text-white">Status</th>
                <th className="text-left p-4 text-white">Submitted</th>
                <th className="text-left p-4 text-white">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredCSRs.map((csr) => (
                <tr key={csr.id} className="border-t border-gray-700">
                  <td className="p-4 text-white">{csr.commonName}</td>
                  <td className="p-4 text-gray-300">{csr.organization}</td>
                  <td className="p-4 text-gray-300">{csr.requesterEmail}</td>
                  <td className="p-4 text-gray-300">{csr.validityDays} days</td>
                  <td className="p-4">{getStatusBadge(csr.status)}</td>
                  <td className="p-4 text-gray-300">
                    {new Date(csr.createdAt).toLocaleDateString()}
                  </td>
                  <td className="p-4">
                    {csr.status === CSRStatus.PENDING ? (
                      <button
                        onClick={() => handleReviewClick(csr)}
                        className="bg-blue-500 hover:bg-blue-600 text-white px-3 py-1 rounded text-sm"
                      >
                        Review
                      </button>
                    ) : (
                      <span className="text-gray-500 text-sm">
                        {csr.status === CSRStatus.APPROVED ? 'Approved' : 'Rejected'}
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {filteredCSRs.length === 0 && (
            <div className="p-8 text-center text-gray-400">
              No CSRs found for selected filter.
            </div>
          )}
        </div>

        {/* Review Modal */}
        {showReviewModal && selectedCSR && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-gray-800 p-6 rounded-lg max-w-2xl w-full mx-4">
              <h2 className="text-2xl font-bold text-white mb-4">Review CSR</h2>

              {/* CSR Details */}
              <div className="bg-gray-700 p-4 rounded mb-4 space-y-2">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <span className="text-gray-400 text-sm">Common Name:</span>
                    <p className="text-white">{selectedCSR.commonName}</p>
                  </div>
                  <div>
                    <span className="text-gray-400 text-sm">Organization:</span>
                    <p className="text-white">{selectedCSR.organization}</p>
                  </div>
                  <div>
                    <span className="text-gray-400 text-sm">Country:</span>
                    <p className="text-white">{selectedCSR.country}</p>
                  </div>
                  <div>
                    <span className="text-gray-400 text-sm">Validity:</span>
                    <p className="text-white">{selectedCSR.validityDays} days</p>
                  </div>
                  <div className="col-span-2">
                    <span className="text-gray-400 text-sm">Requester Email:</span>
                    <p className="text-white">{selectedCSR.requesterEmail}</p>
                  </div>
                </div>
              </div>

              {/* Review Form */}
              <form onSubmit={handleSubmitReview} className="space-y-4">
                <div>
                  <label className="block text-white mb-2">Decision</label>
                  <div className="flex space-x-4">
                    <label className="flex items-center text-white">
                      <input
                        type="radio"
                        checked={reviewForm.approved === true}
                        onChange={() => setReviewForm({ ...reviewForm, approved: true, rejectionReason: '' })}
                        className="mr-2"
                      />
                      Approve
                    </label>
                    <label className="flex items-center text-white">
                      <input
                        type="radio"
                        checked={reviewForm.approved === false}
                        onChange={() => setReviewForm({ ...reviewForm, approved: false, selectedCaSerialNumber: '' })}
                        className="mr-2"
                      />
                      Reject
                    </label>
                  </div>
                </div>

                {reviewForm.approved && (
                  <div>
                    <label className="block text-white mb-2">Select CA for Signing *</label>
                    <select
                      value={reviewForm.selectedCaSerialNumber}
                      onChange={(e) => setReviewForm({ ...reviewForm, selectedCaSerialNumber: e.target.value })}
                      className="w-full p-2 bg-gray-700 text-white rounded"
                      required
                    >
                      <option value="">Select CA Certificate</option>
                      {availableCAs.map(ca => (
                        <option key={ca.serialNumber} value={ca.serialNumber}>
                          {ca.commonName} ({ca.type}) - SN: {ca.serialNumber}
                        </option>
                      ))}
                    </select>
                    <p className="text-gray-400 text-sm mt-1">
                      Choose which CA certificate will be used to sign this certificate
                    </p>
                  </div>
                )}

                {!reviewForm.approved && (
                  <div>
                    <label className="block text-white mb-2">Rejection Reason *</label>
                    <textarea
                      value={reviewForm.rejectionReason}
                      onChange={(e) => setReviewForm({ ...reviewForm, rejectionReason: e.target.value })}
                      className="w-full p-2 bg-gray-700 text-white rounded"
                      rows={3}
                      placeholder="Provide a reason for rejection..."
                      required={!reviewForm.approved}
                    />
                  </div>
                )}

                <div className="flex space-x-4">
                  <button
                    type="submit"
                    className={`${reviewForm.approved ? 'bg-green-600 hover:bg-green-700' : 'bg-red-600 hover:bg-red-700'} text-white px-4 py-2 rounded`}
                  >
                    {reviewForm.approved ? 'Approve CSR' : 'Reject CSR'}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setShowReviewModal(false);
                      setSelectedCSR(null);
                      setError(null);
                    }}
                    className="bg-gray-600 hover:bg-gray-700 text-white px-4 py-2 rounded"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default CSRReview;
