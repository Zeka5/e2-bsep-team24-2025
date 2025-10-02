import React, { useState, useEffect } from 'react';
import { Certificate } from '../types/certificate';
import { certificateService, downloadFile } from '../services/certificateService';

const MyCertificates: React.FC = () => {
  const [certificates, setCertificates] = useState<Certificate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadCertificates();
  }, []);

  const loadCertificates = async () => {
    try {
      setLoading(true);
      const certs = await certificateService.getMyCertificates();
      setCertificates(certs);
    } catch (err) {
      setError('Failed to load certificates');
      console.error(err);
    } finally {
      setLoading(false);
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
          <h1 className="text-3xl font-bold text-white">My Certificates</h1>
        </div>

        {error && (
          <div className="bg-red-600 text-white p-4 rounded mb-6">
            {error}
            <button onClick={() => setError(null)} className="ml-4 text-red-200 hover:text-white">×</button>
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
                    <span className={`px-2 py-1 rounded text-xs ${
                      cert.type === 'ROOT_CA' ? 'bg-red-600' :
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
                        Download PEM
                      </button>
                      <button
                        onClick={() => handleExportCertificate(cert.serialNumber, 'der', cert.commonName)}
                        className="bg-blue-500 hover:bg-blue-600 text-white px-2 py-1 rounded text-xs"
                      >
                        Download DER
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {certificates.length === 0 && (
            <div className="p-8 text-center text-gray-400">
              No certificates found. Submit a CSR to request a certificate.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default MyCertificates;
