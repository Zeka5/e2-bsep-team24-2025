import React, { useState, useEffect } from 'react';
import { Certificate, CreateCertificateRequest, CertificateType } from '../types/certificate';
import { certificateService, downloadFile } from '../services/certificateService';

const CertificateManagement: React.FC = () => {
  const [certificates, setCertificates] = useState<Certificate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showRootForm, setShowRootForm] = useState(false);

  // Form states
  const [createForm, setCreateForm] = useState<CreateCertificateRequest>({
    commonName: '',
    organization: '',
    country: 'RS',
    certificateType: CertificateType.END_ENTITY,
    validityDays: 365,
    parentCaSerialNumber: '',
    subjectAlternativeNames: ['localhost', '127.0.0.1']
  });
  const [rootCaName, setRootCaName] = useState('');

  useEffect(() => {
    loadCertificates();
  }, []);

  const loadCertificates = async () => {
    try {
      setLoading(true);
      const certs = await certificateService.getAllCertificates();
      setCertificates(certs);
    } catch (err) {
      setError('Failed to load certificates');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateRootCA = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await certificateService.createRootCertificate(rootCaName);
      setRootCaName('');
      setShowRootForm(false);
      loadCertificates();
    } catch (err) {
      setError('Failed to create root CA');
      console.error(err);
    }
  };

  const handleCreateCertificate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await certificateService.signCertificate(createForm);
      setCreateForm({
        commonName: '',
        organization: '',
        country: 'RS',
        certificateType: CertificateType.END_ENTITY,
        validityDays: 365,
        parentCaSerialNumber: '',
        subjectAlternativeNames: ['localhost', '127.0.0.1']
      });
      setShowCreateForm(false);
      loadCertificates();
    } catch (err) {
      setError('Failed to create certificate');
      console.error(err);
    }
  };

  const handleExportCertificate = async (serialNumber: string, format: string, commonName: string) => {
    try {
      const blob = await certificateService.exportCertificate(serialNumber, format);
      downloadFile(blob, `${commonName}.${format}`);
    } catch (err) {
      setError(`Failed to export certificate as ${format}`);
      console.error(err);
    }
  };

  const handleExportKeystore = async (serialNumber: string, commonName: string) => {
    const password = prompt('Enter keystore password:');
    if (!password) return;

    try {
      const blob = await certificateService.exportKeystore(serialNumber, password);
      downloadFile(blob, `${commonName}.p12`);
    } catch (err) {
      setError('Failed to export keystore');
      console.error(err);
    }
  };

  const handleDownloadHttpsConfig = async (serialNumber: string, commonName: string) => {
    const password = prompt('Enter keystore password for HTTPS configuration:');
    if (!password) return;

    try {
      const blob = await certificateService.downloadApplicationProperties(serialNumber, password);
      downloadFile(blob, `application-https-${commonName}.properties`);
    } catch (err) {
      setError('Failed to download HTTPS configuration');
      console.error(err);
    }
  };

  const caCertificates = certificates.filter(cert => cert.isCa);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-900 flex items-center justify-center">
        <div className="text-white text-xl">Loading certificates...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900 p-6">
      <div className="max-w-7xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold text-white">Certificate Management</h1>
          <div className="space-x-4">
            <button
              onClick={() => setShowRootForm(true)}
              className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded"
            >
              Create Root CA
            </button>
            <button
              onClick={() => setShowCreateForm(true)}
              className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded"
            >
              Create Certificate
            </button>
          </div>
        </div>

        {error && (
          <div className="bg-red-600 text-white p-4 rounded mb-6">
            {error}
            <button onClick={() => setError(null)} className="ml-4 text-red-200 hover:text-white">×</button>
          </div>
        )}

        {/* Root CA Creation Form */}
        {showRootForm && (
          <div className="bg-gray-800 p-6 rounded mb-6">
            <h2 className="text-xl font-bold text-white mb-4">Create Root CA</h2>
            <form onSubmit={handleCreateRootCA} className="space-y-4">
              <div>
                <label className="block text-white mb-2">Common Name</label>
                <input
                  type="text"
                  value={rootCaName}
                  onChange={(e) => setRootCaName(e.target.value)}
                  className="w-full p-2 bg-gray-700 text-white rounded"
                  placeholder="My Root CA"
                  required
                />
              </div>
              <div className="flex space-x-4">
                <button type="submit" className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded">
                  Create Root CA
                </button>
                <button
                  type="button"
                  onClick={() => setShowRootForm(false)}
                  className="bg-gray-600 hover:bg-gray-700 text-white px-4 py-2 rounded"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Certificate Creation Form */}
        {showCreateForm && (
          <div className="bg-gray-800 p-6 rounded mb-6">
            <h2 className="text-xl font-bold text-white mb-4">Create New Certificate</h2>
            <form onSubmit={handleCreateCertificate} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-white mb-2">Common Name</label>
                  <input
                    type="text"
                    value={createForm.commonName}
                    onChange={(e) => setCreateForm({ ...createForm, commonName: e.target.value })}
                    className="w-full p-2 bg-gray-700 text-white rounded"
                    placeholder="localhost or example.com"
                    required
                  />
                </div>
                <div>
                  <label className="block text-white mb-2">Organization</label>
                  <input
                    type="text"
                    value={createForm.organization}
                    onChange={(e) => setCreateForm({ ...createForm, organization: e.target.value })}
                    className="w-full p-2 bg-gray-700 text-white rounded"
                    placeholder="My Organization"
                    required
                  />
                </div>
                <div>
                  <label className="block text-white mb-2">Country</label>
                  <input
                    type="text"
                    value={createForm.country}
                    onChange={(e) => setCreateForm({ ...createForm, country: e.target.value })}
                    className="w-full p-2 bg-gray-700 text-white rounded"
                    maxLength={2}
                    placeholder="RS"
                    required
                  />
                </div>
                <div>
                  <label className="block text-white mb-2">Certificate Type</label>
                  <select
                    value={createForm.certificateType}
                    onChange={(e) => setCreateForm({ ...createForm, certificateType: e.target.value as CertificateType })}
                    className="w-full p-2 bg-gray-700 text-white rounded"
                  >
                    <option value={CertificateType.END_ENTITY}>End Entity</option>
                    <option value={CertificateType.INTERMEDIATE_CA}>Intermediate CA</option>
                  </select>
                </div>
                <div>
                  <label className="block text-white mb-2">Validity (Days)</label>
                  <input
                    type="number"
                    value={createForm.validityDays}
                    onChange={(e) => setCreateForm({ ...createForm, validityDays: parseInt(e.target.value) })}
                    className="w-full p-2 bg-gray-700 text-white rounded"
                    min="1"
                    required
                  />
                </div>
                <div>
                  <label className="block text-white mb-2">Parent CA</label>
                  <select
                    value={createForm.parentCaSerialNumber}
                    onChange={(e) => setCreateForm({ ...createForm, parentCaSerialNumber: e.target.value })}
                    className="w-full p-2 bg-gray-700 text-white rounded"
                    required
                  >
                    <option value="">Select Parent CA</option>
                    {caCertificates.map(ca => (
                      <option key={ca.serialNumber} value={ca.serialNumber}>
                        {ca.commonName} ({ca.serialNumber})
                      </option>
                    ))}
                  </select>
                </div>
              </div>
              <div>
                <label className="block text-white mb-2">Subject Alternative Names (for HTTPS)</label>
                <input
                  type="text"
                  value={createForm.subjectAlternativeNames?.join(', ')}
                  onChange={(e) => setCreateForm({
                    ...createForm,
                    subjectAlternativeNames: e.target.value.split(',').map(s => s.trim()).filter(Boolean)
                  })}
                  className="w-full p-2 bg-gray-700 text-white rounded"
                  placeholder="localhost, 127.0.0.1, ::1, example.com"
                />
                <p className="text-gray-400 text-sm mt-1">
                  Comma-separated list. Include localhost, 127.0.0.1 for local HTTPS testing
                </p>
              </div>
              <div className="flex space-x-4">
                <button type="submit" className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded">
                  Create Certificate
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

        {/* Certificates List */}
        <div className="bg-gray-800 rounded overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-700">
              <tr>
                <th className="text-left p-4 text-white">Common Name</th>
                <th className="text-left p-4 text-white">Type</th>
                <th className="text-left p-4 text-white">Serial Number</th>
                <th className="text-left p-4 text-white">Valid Until</th>
                <th className="text-left p-4 text-white">Actions</th>
              </tr>
            </thead>
            <tbody>
              {certificates.map((cert) => (
                <tr key={cert.serialNumber} className="border-t border-gray-700">
                  <td className="p-4 text-white">{cert.commonName}</td>
                  <td className="p-4 text-white">
                    <span className={`px-2 py-1 rounded text-xs ${cert.type === 'ROOT_CA' ? 'bg-red-600' :
                      cert.type === 'INTERMEDIATE_CA' ? 'bg-yellow-600' : 'bg-green-600'
                      }`}>
                      {cert.type}
                    </span>
                  </td>
                  <td className="p-4 text-gray-300 font-mono text-sm">{cert.serialNumber}</td>
                  <td className="p-4 text-gray-300">
                    {new Date(cert.notAfter).toLocaleDateString()}
                  </td>
                  <td className="p-4">
                    <div className="flex flex-wrap gap-2">
                      <button
                        onClick={() => handleExportCertificate(cert.serialNumber, 'pem', cert.commonName)}
                        className="bg-blue-500 hover:bg-blue-600 text-white px-2 py-1 rounded text-xs"
                      >
                        PEM
                      </button>
                      <button
                        onClick={() => handleExportCertificate(cert.serialNumber, 'der', cert.commonName)}
                        className="bg-blue-500 hover:bg-blue-600 text-white px-2 py-1 rounded text-xs"
                      >
                        DER
                      </button>
                      {cert.isCa && (
                        <>
                          <button
                            onClick={() => handleExportKeystore(cert.serialNumber, cert.commonName)}
                            className="bg-purple-500 hover:bg-purple-600 text-white px-2 py-1 rounded text-xs"
                          >
                            Keystore
                          </button>
                          <button
                            onClick={() => handleDownloadHttpsConfig(cert.serialNumber, cert.commonName)}
                            className="bg-green-500 hover:bg-green-600 text-white px-2 py-1 rounded text-xs"
                          >
                            HTTPS Config
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {certificates.length === 0 && (
            <div className="p-8 text-center text-gray-400">
              No certificates found. Create a Root CA to get started.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default CertificateManagement;