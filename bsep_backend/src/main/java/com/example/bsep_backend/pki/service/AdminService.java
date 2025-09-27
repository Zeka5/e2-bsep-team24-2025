package com.example.bsep_backend.pki.service;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.pki.domain.Certificate;
import com.example.bsep_backend.pki.domain.CertificateType;
import com.example.bsep_backend.pki.domain.Issuer;
import com.example.bsep_backend.pki.domain.Subject;
import com.example.bsep_backend.pki.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.x500.X500Name;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final CertificateRepository certificateRepository;
    private final CertificateGenerator certificateGenerator;
    private final KeyStoreService keyStoreService;

    public Certificate createRootCertificate(User admin, String commonName) throws Exception {
        KeyPair keyPair = certificateGenerator.generateKeyPair();
        LocalDateTime notBefore = LocalDateTime.now();
        LocalDateTime notAfter = notBefore.plusYears(10);
        String serialNumber = generateSerialNumber();

        String subjectDN = String.format("CN=%s,O=PKI Organization,C=RS", commonName);
        X500Name x500Name = new X500Name(subjectDN);

        Subject subject = new Subject(keyPair.getPublic(), x500Name);
        Issuer issuer = new Issuer(keyPair.getPrivate(), keyPair.getPublic(), x500Name);

        X509Certificate x509Certificate = certificateGenerator.generateCertificate(
                subject, issuer, notBefore, notAfter, serialNumber
        );

        Certificate certificate = Certificate.builder()
                .serialNumber(serialNumber)
                .commonName(commonName)
                .notBefore(notBefore)
                .notAfter(notAfter)
                .type(CertificateType.ROOT_CA)
                .isCa(true)
                .certificateData(Base64.getEncoder().encodeToString(x509Certificate.getEncoded()))
                .owner(admin)
                .build();

        Certificate savedCertificate = certificateRepository.save(certificate);

        keyStoreService.saveCAKeyStore(serialNumber, keyPair.getPrivate(), x509Certificate);

        return savedCertificate;
    }
    public java.util.List<Certificate> getAllCertificates() {
        return certificateRepository.findAll();
    }

    private String generateSerialNumber() {
        return String.valueOf(System.currentTimeMillis());
    }
}