package com.example.bsep_backend.passwordmanager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class RSAPasswordEncryptionService {

    /**
     * Enkriptuje lozinku koristeći javni ključ iz sertifikata
     * @param password Plain text lozinka
     * @param base64CertData Base64 enkodovani X509 sertifikat iz baze
     * @return Base64 enkodovana enkriptovana lozinka
     */
    public String encryptPassword(String password, String base64CertData) throws Exception {
        log.info("=== Starting password encryption ===");
        log.info("Password length: {}", password.length());
        log.info("Certificate data length: {}", base64CertData.length());

        // Izvuci X509 sertifikat iz Base64 stringa
        X509Certificate certificate = getX509Certificate(base64CertData);
        log.info("Certificate parsed successfully. Subject: {}", certificate.getSubjectX500Principal().getName());

        // Izvuci javni ključ iz sertifikata
        PublicKey publicKey = certificate.getPublicKey();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        log.info("Public key extracted. Algorithm: {}, Format: {}",
                publicKey.getAlgorithm(), publicKey.getFormat());
        log.info("Public key modulus (first 200 chars): {}", publicKey.toString().substring(0, Math.min(200, publicKey.toString().length())));
        log.info("Public key (Base64 DER format): {}", publicKeyBase64);

        // Enkriptuj lozinku koristeći RSA-OAEP sa SHA-1 (default za Web Crypto API)
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        log.info("Cipher created with transformation: RSA/ECB/OAEPWithSHA-1AndMGF1Padding");

        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        log.info("Cipher initialized in ENCRYPT_MODE");

        byte[] passwordBytes = password.getBytes("UTF-8");
        log.info("Password bytes length: {}", passwordBytes.length);

        byte[] encryptedBytes = cipher.doFinal(passwordBytes);
        log.info("Encryption completed. Encrypted bytes length: {}", encryptedBytes.length);

        // Vrati enkriptovanu lozinku kao Base64 string
        String encryptedPassword = Base64.getEncoder().encodeToString(encryptedBytes);
        log.info("Base64 encoded encrypted password length: {}", encryptedPassword.length());
        log.info("=== Password encryption completed successfully ===");

        return encryptedPassword;
    }

    private X509Certificate getX509Certificate(String base64CertData) throws Exception {
        byte[] certBytes = Base64.getDecoder().decode(base64CertData);
        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(certBytes));
    }
}
