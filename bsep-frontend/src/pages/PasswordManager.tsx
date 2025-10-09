import React, { useState, useEffect } from 'react';
import { passwordManagerService } from '../services/passwordManagerService';
import { certificateService } from '../services/certificateService';
import { PasswordEntry, CreatePasswordEntryRequest } from '../types/passwordEntry';
import { Certificate } from '../types/certificate';
import { decryptPassword, readFileAsText } from '../utils/cryptoUtils';

const PasswordManager: React.FC = () => {
  const [passwordEntries, setPasswordEntries] = useState<PasswordEntry[]>([]);
  const [certificates, setCertificates] = useState<Certificate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // State za novu password entry
  const [newEntry, setNewEntry] = useState<CreatePasswordEntryRequest>({
    website: '',
    username: '',
    password: '',
    certificateSerialNumber: '',
  });

  // State za privatni ključ i dekriptovane lozinke
  const [privateKey, setPrivateKey] = useState<string | null>(null);
  const [decryptedPasswords, setDecryptedPasswords] = useState<{ [id: number]: string }>({});
  const [dragActive, setDragActive] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [entries, certs] = await Promise.all([
        passwordManagerService.getPasswordEntries(),
        certificateService.getMyCertificates(),
      ]);
      setPasswordEntries(entries);
      // Filtriraj samo END_ENTITY sertifikate
      setCertificates(certs.filter((cert) => cert.type === 'END_ENTITY'));
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Greška pri učitavanju podataka');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateEntry = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await passwordManagerService.createPasswordEntry(newEntry);
      setSuccess('Password entry uspešno kreiran!');
      setNewEntry({
        website: '',
        username: '',
        password: '',
        certificateSerialNumber: '',
      });
      fetchData();
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Greška pri kreiranju password entry-ja');
    }
  };

  const handleDeleteEntry = async (id: number) => {
    if (!window.confirm('Da li ste sigurni da želite da obrišete ovaj entry?')) {
      return;
    }
    try {
      await passwordManagerService.deletePasswordEntry(id);
      setSuccess('Password entry uspešno obrisan!');
      fetchData();
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Greška pri brisanju password entry-ja');
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
    for (const entry of passwordEntries) {
      try {
        const decryptedPassword = await decryptPassword(entry.encryptedPassword, key);
        decrypted[entry.id] = decryptedPassword;
      } catch (err) {
        console.error(`Failed to decrypt password for entry ${entry.id}:`, err);
        decrypted[entry.id] = 'Greška pri dekripciji';
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
        <h1 className="text-3xl font-bold text-white mb-8">Password Manager</h1>

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

        {/* Forma za dodavanje novog entry-ja */}
        <div className="bg-gray-800 p-6 rounded-lg shadow-lg mb-8">
          <h2 className="text-xl font-bold text-white mb-4">Dodaj novu lozinku</h2>
          <form onSubmit={handleCreateEntry} className="space-y-4">
            <div>
              <label className="block text-gray-300 mb-2">Naziv sajta</label>
              <input
                type="text"
                value={newEntry.website}
                onChange={(e) => setNewEntry({ ...newEntry, website: e.target.value })}
                className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:outline-none focus:border-blue-500"
                required
              />
            </div>
            <div>
              <label className="block text-gray-300 mb-2">Korisničko ime</label>
              <input
                type="text"
                value={newEntry.username}
                onChange={(e) => setNewEntry({ ...newEntry, username: e.target.value })}
                className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:outline-none focus:border-blue-500"
                required
              />
            </div>
            <div>
              <label className="block text-gray-300 mb-2">Lozinka</label>
              <input
                type="password"
                value={newEntry.password}
                onChange={(e) => setNewEntry({ ...newEntry, password: e.target.value })}
                className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:outline-none focus:border-blue-500"
                required
              />
            </div>
            <div>
              <label className="block text-gray-300 mb-2">Sertifikat</label>
              <select
                value={newEntry.certificateSerialNumber}
                onChange={(e) =>
                  setNewEntry({ ...newEntry, certificateSerialNumber: e.target.value })
                }
                className="w-full px-4 py-2 bg-gray-700 text-white rounded border border-gray-600 focus:outline-none focus:border-blue-500"
                required
              >
                <option value="">Izaberi sertifikat</option>
                {certificates.map((cert) => (
                  <option key={cert.serialNumber} value={cert.serialNumber}>
                    {cert.commonName} ({cert.serialNumber})
                  </option>
                ))}
              </select>
              {certificates.length === 0 && (
                <p className="text-yellow-400 text-sm mt-2">
                  Nemate dostupne END_ENTITY sertifikate. Kreirajte sertifikat prvo.
                </p>
              )}
            </div>
            <button
              type="submit"
              className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded transition duration-200"
              disabled={certificates.length === 0}
            >
              Dodaj lozinku
            </button>
          </form>
        </div>

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
        </div>

        {/* Tabela sa password entries */}
        <div className="bg-gray-800 rounded-lg shadow-lg overflow-hidden">
          <h2 className="text-xl font-bold text-white p-6 pb-4">Sačuvane lozinke</h2>
          {passwordEntries.length === 0 ? (
            <div className="text-gray-400 text-center py-8">
              Nemate sačuvanih lozinki
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
                      Sertifikat
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                      Akcije
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-700">
                  {passwordEntries.map((entry) => (
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
                        {entry.certificateCommonName}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm">
                        <button
                          onClick={() => handleDeleteEntry(entry.id)}
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

        {!privateKey && passwordEntries.length > 0 && (
          <div className="mt-4 bg-yellow-900 bg-opacity-50 border border-yellow-600 text-yellow-200 px-4 py-3 rounded">
            <strong>Napomena:</strong> Učitajte privatni ključ da biste videli dekriptovane lozinke.
          </div>
        )}
      </div>
    </div>
  );
};

export default PasswordManager;
