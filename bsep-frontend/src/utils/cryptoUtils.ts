/**
 * Parsuje PEM format privatnog ključa i dekriptuje enkriptovanu lozinku
 * @param encryptedPasswordBase64 Base64 enkodovana enkriptovana lozinka
 * @param privateKeyPem Privatni ključ u PEM formatu
 * @returns Dekriptovana lozinka kao string
 */
export async function decryptPassword(
  encryptedPasswordBase64: string,
  privateKeyPem: string
): Promise<string> {
  console.log('=== Starting password decryption ===');
  console.log('Encrypted password (Base64):', encryptedPasswordBase64);
  console.log('Encrypted password length:', encryptedPasswordBase64.length);

  try {
    // Ukloni PEM header i footer
    const pemHeader = '-----BEGIN PRIVATE KEY-----';
    const pemFooter = '-----END PRIVATE KEY-----';
    const rsaHeader = '-----BEGIN RSA PRIVATE KEY-----';
    const rsaFooter = '-----END RSA PRIVATE KEY-----';

    console.log('Private key PEM (first 100 chars):', privateKeyPem.substring(0, 100));

    let pemContents = privateKeyPem
      .replace(pemHeader, '')
      .replace(pemFooter, '')
      .replace(rsaHeader, '')
      .replace(rsaFooter, '')
      .replace(/\s/g, '');

    console.log('PEM contents length after cleanup:', pemContents.length);

    // Dekoduj Base64 privatni ključ
    const binaryDer = atob(pemContents);
    const binaryDerArray = new Uint8Array(binaryDer.length);
    for (let i = 0; i < binaryDer.length; i++) {
      binaryDerArray[i] = binaryDer.charCodeAt(i);
    }

    console.log('Binary DER array length:', binaryDerArray.length);

    // Import privatnog ključa koristeći Web Crypto API
    let cryptoKey: CryptoKey;
    try {
      console.log('Attempting to import private key as PKCS8...');
      // Pokušaj da importuješ kao PKCS8 (standardni format)
      cryptoKey = await window.crypto.subtle.importKey(
        'pkcs8',
        binaryDerArray.buffer,
        {
          name: 'RSA-OAEP',
          hash: 'SHA-1',
        },
        true, // extractable = true da bismo mogli da izvučemo javni ključ
        ['decrypt']
      );
      console.log('Successfully imported private key as PKCS8');

      // Ekstraktuj javni ključ iz privatnog za proveru
      const jwk = await window.crypto.subtle.exportKey('jwk', cryptoKey);
      console.log('Private key JWK (n - modulus):', jwk.n?.substring(0, 100));

    } catch (pkcs8Error) {
      console.error('Failed to import as PKCS8:', pkcs8Error);
      // Ako PKCS8 ne uspe, ključ je verovatno u PKCS1 formatu
      // Za PKCS1 format (RSA PRIVATE KEY), potrebna je konverzija
      // Web Crypto API ne podržava direktno PKCS1, pa ćemo koristiti alternativan pristup
      throw new Error(
        'Privatni ključ mora biti u PKCS#8 formatu. Konvertujte ga koristeći: openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in server.key -out server_pkcs8.key'
      );
    }

    // Dekoduj enkriptovanu lozinku iz Base64
    console.log('Decoding encrypted password from Base64...');
    const encryptedData = atob(encryptedPasswordBase64);
    const encryptedArray = new Uint8Array(encryptedData.length);
    for (let i = 0; i < encryptedData.length; i++) {
      encryptedArray[i] = encryptedData.charCodeAt(i);
    }
    console.log('Encrypted data length:', encryptedArray.length);

    // Dekriptuj koristeći RSA-OAEP
    console.log('Attempting to decrypt using RSA-OAEP...');
    const decryptedBuffer = await window.crypto.subtle.decrypt(
      {
        name: 'RSA-OAEP',
      },
      cryptoKey,
      encryptedArray.buffer
    );
    console.log('Decryption successful!');

    // Konvertuj dekriptovane bajtove u string
    const decoder = new TextDecoder();
    const decryptedPassword = decoder.decode(decryptedBuffer);

    console.log('Decrypted password length:', decryptedPassword.length);
    console.log('=== Password decryption completed successfully ===');

    return decryptedPassword;
  } catch (error) {
    console.error('=== Error decrypting password ===');
    console.error('Error type:', error?.constructor?.name);
    console.error('Error message:', error instanceof Error ? error.message : String(error));
    console.error('Full error:', error);
    throw new Error(
      `Greška pri dekripciji lozinke: ${error instanceof Error ? error.message : 'Nepoznata greška'}`
    );
  }
}

/**
 * Čita sadržaj fajla kao tekst
 * @param file File objekat
 * @returns Promise sa sadržajem fajla kao string
 */
export function readFileAsText(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const text = e.target?.result as string;
      resolve(text);
    };
    reader.onerror = (e) => {
      reject(new Error('Error reading file'));
    };
    reader.readAsText(file);
  });
}
