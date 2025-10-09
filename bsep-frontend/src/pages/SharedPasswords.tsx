import React, { useState, useEffect } from 'react';
import { sharedPasswordService } from '../services/sharedPasswordService';
import { SharedPasswordEntry } from '../types/sharedPassword';
import { decryptPassword, readFileAsText } from '../utils/cryptoUtils';

const SharedPasswords: React.FC = () => {
  const [sharedPasswords, setSharedPasswords] = useState<SharedPasswordEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // State za privatni ključ i dekriptovane lozinke
  const [privateKey, setPrivateKey] = useState<string | null>(null);
  const [decryptedPasswords, setDecryptedPasswords] = useState<{ [id: number]: string }>({});
  const [dragActive, setDragActive] = useState(false);

  useEffect(() => {
    fetchSharedPasswords();
  }, []);

  const fetchSharedPasswords = async () => {
    try {
      setLoading(true);
      const passwords = await sharedPasswordService.getSharedPasswords();
      setSharedPasswords(passwords);
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Greška pri učitavanju podeljenih lozinki');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteSharedPassword = async (sharedEntryId: number) => {
    if (!window.confirm('Da li ste sigurni da želite da obrišete ovu podeljenu lozinku?')) {
      return;
    }
    try {
      await sharedPasswordService.deleteSharedPassword(sharedEntryId);
      setSuccess('Podeljena lozinka uspešno obrisana!');
      fetchSharedPasswords();
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Greška pri brisanju podeljene lozinke');
    }
  };

  const handleFileUpload = async (file: File) => {
    try {
      const keyContent = await readFileAsText(file);
      setPrivateKey(keyContent);
      setSuccess('Privatni ključ uspešno učitan!');
      setTimeout(() => setSuccess(null), 3000);

      // Automatski dekriptuj sve lozinke
      await decryptAllPasswords(keyContent);
    } catch (err: any) {
      setError('Greška pri čitanju privatnog ključa');
      console.error(err);
    }
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileUpload(e.dataTransfer.files[0]);
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      handleFileUpload(e.target.files[0]);
    }
  };

  const decryptAllPasswords = async (key: string) => {
    const decrypted: { [id: number]: string } = {};
    for (const entry of sharedPasswords) {
      try {
        const decryptedPassword = await decryptPassword(entry.encryptedPassword, key);
        decrypted[entry.id] = decryptedPassword;
      } catch (err) {
        console.error(`Failed to decrypt password for entry ${entry.id}:`, err);
        decrypted[entry.id] = 'Greška pri dekripciji - koristite privatni ključ koji odgovara sertifikatu';
      }
    }
    setDecryptedPasswords(decrypted);
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setSuccess('Lozinka kopirana u clipboard!');
    setTimeout(() => setSuccess(null), 2000);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-900 flex items-center justify-center">
        <div className="text-white text-xl">Učitavanje...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900 py-8 px-4">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-bold text-white mb-8">Podeljene lozinke</h1>

        {error && (
          <div className="bg-red-500 text-white px-4 py-3 rounded mb-4">
            {error}
            <button onClick={() => setError(null)} className="float-right font-bold">
              ×
            </button>
          </div>
        )}

        {success && (
          <div className="bg-green-500 text-white px-4 py-3 rounded mb-4">
            {success}
          </div>
        )}

        {/* Drag & Drop zona za privatni ključ */}
        <div className="bg-gray-800 p-6 rounded-lg shadow-lg mb-8">
          <h2 className="text-xl font-bold text-white mb-4">Učitaj privatni ključ</h2>
          <div
            className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
              dragActive
                ? 'border-blue-500 bg-blue-900 bg-opacity-20'
                : 'border-gray-600 bg-gray-700'
            }`}
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
          >
            <input
              type="file"
              accept=".key,.pem"
              onChange={handleFileInputChange}
              className="hidden"
              id="keyFileInput"
            />
            <label htmlFor="keyFileInput" className="cursor-pointer">
              <div className="text-gray-300">
                {privateKey ? (
                  <div className="text-green-400">
                    ✓ Privatni ključ učitan - lozinke dekriptovane
                  </div>
                ) : (
                  <>
                    <div className="text-lg mb-2">
                      Prevucite .key fajl ovde ili kliknite da izaberete
                    </div>
                    <div className="text-sm text-gray-400">
                      Privatni ključ se koristi samo na klijentskoj strani i neće biti poslat na server
                    </div>
                  </>
                )}
              </div>
            </label>
          </div>
          <div className="mt-3 text-sm text-yellow-300">
            <strong>Napomena:</strong> Koristite privatni ključ koji odgovara sertifikatu kojim je lozinka enkriptovana
          </div>
        </div>

        {/* Tabela sa podeljenim lozinkama */}
        <div className="bg-gray-800 rounded-lg shadow-lg overflow-hidden">
          <h2 className="text-xl font-bold text-white p-6 pb-4">Lozinke podeljene sa mnom</h2>
          {sharedPasswords.length === 0 ? (
            <div className="text-gray-400 text-center py-8">
              Nema podeljenih lozinki
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-700">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                      Sajt
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                      Korisničko ime
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                      Lozinka
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                      Podelio
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                      Sertifikat
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                      Datum
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                      Akcije
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-700">
                  {sharedPasswords.map((entry) => (
                    <tr key={entry.id} className="hover:bg-gray-700">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-white">
                        {entry.website}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-white">
                        {entry.username}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-white">
                        {privateKey && decryptedPasswords[entry.id] ? (
                          <div className="flex items-center space-x-2">
                            <span className="font-mono">{decryptedPasswords[entry.id]}</span>
                            <button
                              onClick={() => copyToClipboard(decryptedPasswords[entry.id])}
                              className="text-blue-400 hover:text-blue-300"
                              title="Kopiraj lozinku"
                            >
                              📋
                            </button>
                          </div>
                        ) : (
                          <span className="text-gray-500">••••••••</span>
                        )}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-300">
                        <div>{entry.sharedByUserName}</div>
                        <div className="text-xs text-gray-400">{entry.sharedByUserEmail}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-300">
                        <div>{entry.sharedWithCertificateCommonName}</div>
                        <div className="text-xs text-gray-400">{entry.sharedWithCertificateSerialNumber}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-400">
                        {new Date(entry.createdAt).toLocaleDateString('sr-RS', {
                          year: 'numeric',
                          month: '2-digit',
                          day: '2-digit',
                          hour: '2-digit',
                          minute: '2-digit'
                        })}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm">
                        <button
                          onClick={() => handleDeleteSharedPassword(entry.id)}
                          className="text-red-400 hover:text-red-300 font-medium"
                        >
                          Obriši
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {!privateKey && sharedPasswords.length > 0 && (
          <div className="mt-4 bg-yellow-900 bg-opacity-50 border border-yellow-600 text-yellow-200 px-4 py-3 rounded">
            <strong>Napomena:</strong> Učitajte privatni ključ da biste videli dekriptovane lozinke.
          </div>
        )}
      </div>
    </div>
  );
};

export default SharedPasswords;
