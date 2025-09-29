package com.example.bsep_backend.pki.service.impl;

import com.example.bsep_backend.exception.NotFoundException;
import com.example.bsep_backend.pki.domain.Certificate;
import com.example.bsep_backend.pki.dto.CertificateExportResponse;
import com.example.bsep_backend.pki.repository.CertificateRepository;
import com.example.bsep_backend.pki.service.CertificateExportService;
import com.example.bsep_backend.pki.service.KeyStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateExportServiceImpl implements CertificateExportService {

    private final CertificateRepository certificateRepository;
    private final KeyStoreService keyStoreService;

    @Override
    public CertificateExportResponse exportCertificate(String serialNumber, String format) {
        Certificate certificate = certificateRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new NotFoundException("Certificate not found"));

        try {
            byte[] certData = Base64.getDecoder().decode(certificate.getCertificateData());

            return switch (format.toLowerCase()) {
                case "der" -> CertificateExportResponse.builder()
                        .filename(certificate.getCommonName() + ".crt")
                        .contentType("application/x-x509-ca-cert")
                        .content(certData)
                        .format("DER")
                        .build();

                case "pem" -> {
                    String pemContent = "-----BEGIN CERTIFICATE-----\n" +
                            Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(certData) +
                            "\n-----END CERTIFICATE-----";
                    yield CertificateExportResponse.builder()
                            .filename(certificate.getCommonName() + ".pem")
                            .contentType("application/x-pem-file")
                            .content(pemContent.getBytes())
                            .format("PEM")
                            .build();
                }

                case "crt" -> CertificateExportResponse.builder()
                        .filename(certificate.getCommonName() + ".crt")
                        .contentType("application/x-x509-ca-cert")
                        .content(certData)
                        .format("CRT")
                        .build();

                default -> throw new IllegalArgumentException("Unsupported format: " + format + "try der, pem or crt");
            };

        } catch (Exception e) {
            log.error("Error exporting certificate {}: {}", serialNumber, e.getMessage());
            throw new RuntimeException("Failed to export certificate", e);
        }
    }

    @Override
    public CertificateExportResponse exportKeystore(String serialNumber, String keystorePassword) {
        Certificate certificate = certificateRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new NotFoundException("Certificate not found"));

        if (!certificate.isCa()) {
            throw new IllegalArgumentException("Only CA certificates can be exported as keystores");
        }

        try {
            PrivateKey privateKey = keyStoreService.getPrivateKey(serialNumber);
            byte[] certData = Base64.getDecoder().decode(certificate.getCertificateData());

            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            X509Certificate x509Cert = (X509Certificate) cf.generateCertificate(
                    new java.io.ByteArrayInputStream(certData));

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);

            java.security.cert.Certificate[] certChain = {x509Cert};
            keyStore.setKeyEntry("server", privateKey, keystorePassword.toCharArray(), certChain);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            keyStore.store(baos, keystorePassword.toCharArray());

            return CertificateExportResponse.builder()
                    .filename(certificate.getCommonName() + ".p12")
                    .contentType("application/x-pkcs12")
                    .content(baos.toByteArray())
                    .format("PKCS12")
                    .build();

        } catch (Exception e) {
            log.error("Error exporting keystore for certificate {}: {}", serialNumber, e.getMessage());
            throw new RuntimeException("Failed to export keystore", e);
        }
    }
}