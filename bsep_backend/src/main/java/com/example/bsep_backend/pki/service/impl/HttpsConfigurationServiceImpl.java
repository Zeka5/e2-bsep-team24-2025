package com.example.bsep_backend.pki.service.impl;

import com.example.bsep_backend.exception.NotFoundException;
import com.example.bsep_backend.pki.domain.Certificate;
import com.example.bsep_backend.pki.dto.HttpsConfigurationResponse;
import com.example.bsep_backend.pki.repository.CertificateRepository;
import com.example.bsep_backend.pki.service.CertificateExportService;
import com.example.bsep_backend.pki.service.HttpsConfigurationService;
import com.example.bsep_backend.pki.service.KeyStoreService;
import com.example.bsep_backend.pki.dto.CertificateExportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HttpsConfigurationServiceImpl implements HttpsConfigurationService {

    private final CertificateRepository certificateRepository;
    private final CertificateExportService certificateExportService;
    private final KeyStoreService keyStoreService;

    @Override
    public HttpsConfigurationResponse generateSpringBootSslConfig(String certificateSerialNumber, String keystorePassword) {
        Certificate certificate = certificateRepository.findBySerialNumber(certificateSerialNumber)
                .orElseThrow(() -> new NotFoundException("Certificate not found"));

        if (!certificate.isCa()) {
            throw new IllegalArgumentException("Only CA certificates can be used for HTTPS configuration");
        }

        try {
            // Create a new PKCS12 keystore with the user-provided password
            var keystoreResponse = createCustomKeystore(certificateSerialNumber, keystorePassword);

            X509Certificate x509Cert = getX509Certificate(certificate.getCertificateData());
            Collection<List<?>> sanCollection = x509Cert.getSubjectAlternativeNames();
            String[] sans = extractSubjectAlternativeNames(sanCollection);

            String applicationProperties = generateApplicationProperties(certificateSerialNumber, keystorePassword, 8443);

            String certificateInfo = String.format(
                "Certificate: %s\nSerial Number: %s\nValid From: %s\nValid Until: %s\nIssuer: %s",
                certificate.getCommonName(),
                certificate.getSerialNumber(),
                certificate.getNotBefore(),
                certificate.getNotAfter(),
                certificate.getIssuer() != null ? certificate.getIssuer().getCommonName() : "Self-signed"
            );

            return HttpsConfigurationResponse.builder()
                    .applicationProperties(applicationProperties)
                    .keystoreFile(keystoreResponse.getContent())
                    .keystoreFilename(keystoreResponse.getFilename())
                    .certificateInfo(certificateInfo)
                    .subjectAlternativeNames(sans)
                    .commonName(certificate.getCommonName())
                    .validityPeriod(certificate.getNotBefore() + " to " + certificate.getNotAfter())
                    .build();

        } catch (Exception e) {
            log.error("Error generating HTTPS configuration for certificate {}: {}", certificateSerialNumber, e.getMessage());
            throw new RuntimeException("Failed to generate HTTPS configuration", e);
        }
    }

    @Override
    public String generateApplicationProperties(String certificateSerialNumber, String keystorePassword, int port) {
        Certificate certificate = certificateRepository.findBySerialNumber(certificateSerialNumber)
                .orElseThrow(() -> new NotFoundException("Certificate not found"));

        String keystoreFilename = certificate.getCommonName() + ".p12";

        return String.format("""
                # HTTPS SSL Configuration
                server.port=%d
                server.ssl.enabled=true
                server.ssl.key-store=classpath:keystore/%s
                server.ssl.key-store-password=%s
                server.ssl.key-store-type=PKCS12
                server.ssl.key-alias=server

                # Optional: Require HTTPS
                server.ssl.require-ssl=true

                # Security Headers
                server.ssl.client-auth=none

                # TLS Configuration
                server.ssl.protocol=TLS
                server.ssl.enabled-protocols=TLSv1.2,TLSv1.3

                # Certificate Information
                # Common Name: %s
                # Serial Number: %s
                # Valid From: %s
                # Valid Until: %s
                """,
                port,
                keystoreFilename,
                keystorePassword,
                certificate.getCommonName(),
                certificate.getSerialNumber(),
                certificate.getNotBefore(),
                certificate.getNotAfter()
        );
    }

    @Override
    public void exportHttpsConfigurationBundle(String certificateSerialNumber, String keystorePassword, String outputPath) throws Exception {
        HttpsConfigurationResponse config = generateSpringBootSslConfig(certificateSerialNumber, keystorePassword);
        log.info("HTTPS configuration bundle would be exported to: {}", outputPath);
    }

    private X509Certificate getX509Certificate(String base64CertData) throws Exception {
        byte[] certBytes = Base64.getDecoder().decode(base64CertData);
        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(certBytes));
    }

    private String[] extractSubjectAlternativeNames(Collection<List<?>> sanCollection) {
        if (sanCollection == null) {
            return new String[0];
        }

        return sanCollection.stream()
                .filter(san -> san.size() >= 2 && san.get(0).equals(2))
                .map(san -> san.get(1).toString())
                .toArray(String[]::new);
    }

    private CertificateExportResponse createCustomKeystore(String serialNumber, String password) throws Exception {
        Certificate certificate = certificateRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new NotFoundException("Certificate not found"));

        // Get the private key from the existing keystore
        PrivateKey privateKey = keyStoreService.getPrivateKey(serialNumber);
        byte[] certData = Base64.getDecoder().decode(certificate.getCertificateData());

        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        X509Certificate x509Cert = (X509Certificate) cf.generateCertificate(
                new java.io.ByteArrayInputStream(certData));

        // Create new PKCS12 keystore with user-provided password
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);

        java.security.cert.Certificate[] certChain = {x509Cert};
        keyStore.setKeyEntry("server", privateKey, password.toCharArray(), certChain);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        keyStore.store(baos, password.toCharArray());

        return CertificateExportResponse.builder()
                .filename(certificate.getCommonName() + ".p12")
                .contentType("application/x-pkcs12")
                .content(baos.toByteArray())
                .format("PKCS12")
                .build();
    }
}