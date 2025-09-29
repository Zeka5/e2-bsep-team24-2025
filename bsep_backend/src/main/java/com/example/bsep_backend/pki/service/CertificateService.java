package com.example.bsep_backend.pki.service;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.exception.NotFoundException;
import com.example.bsep_backend.pki.domain.Certificate;
import com.example.bsep_backend.pki.domain.CertificateType;
import com.example.bsep_backend.pki.domain.Issuer;
import com.example.bsep_backend.pki.domain.Subject;
import com.example.bsep_backend.pki.dto.CreateCertificateRequest;
import com.example.bsep_backend.pki.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

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
                .createdAt(LocalDateTime.now())
                .build();

        Certificate savedCertificate = certificateRepository.save(certificate);

        keyStoreService.saveCAKeyStore(serialNumber, keyPair.getPrivate(), x509Certificate, admin);

        return savedCertificate;
    }
    public java.util.List<Certificate> getAllCertificates() {
        return certificateRepository.findAllWithOwnerAndIssuer();
    }

    public Certificate signCertificate(CreateCertificateRequest request, User requestingUser) throws Exception {
        Certificate parentCa = certificateRepository.findBySerialNumber(request.getParentCaSerialNumber())
                .orElseThrow(() -> new NotFoundException("Parent CA certificate not found"));

        if (!parentCa.isCa()) {
            throw new IllegalArgumentException("Parent certificate is not a CA certificate");
        }

        if (parentCa.getNotAfter().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Parent CA certificate has expired");
        }

        PrivateKey parentPrivateKey = keyStoreService.getPrivateKey(parentCa.getSerialNumber());
        X509Certificate parentX509Cert = getX509Certificate(parentCa.getCertificateData());

        KeyPair newKeyPair = certificateGenerator.generateKeyPair();

        LocalDateTime notBefore = LocalDateTime.now();
        LocalDateTime notAfter = notBefore.plusDays(request.getValidityDays());

        if (notAfter.isAfter(parentCa.getNotAfter())) {
            notAfter = parentCa.getNotAfter();
            log.warn("Certificate validity adjusted to parent CA expiration date: {}", notAfter);
        }

        String subjectDN = String.format("CN=%s,O=%s,C=%s",
            request.getCommonName(), request.getOrganization(), request.getCountry());
        X500Name subjectX500Name = new X500Name(subjectDN);

        X500Name issuerX500Name = new X500Name(parentX509Cert.getSubjectDN().getName());

        String serialNumber = generateSerialNumber();
        boolean isCA = request.getCertificateType() == CertificateType.INTERMEDIATE_CA;

        Subject subject = new Subject(newKeyPair.getPublic(), subjectX500Name);
        Issuer issuer = new Issuer(parentPrivateKey, parentX509Cert.getPublicKey(), issuerX500Name);

        X509Certificate signedX509Cert = certificateGenerator.generateCertificateWithSAN(
                subject, issuer, notBefore, notAfter, serialNumber,
                request.getSubjectAlternativeNames(), isCA);

        Certificate certificate = Certificate.builder()
                .serialNumber(serialNumber)
                .commonName(request.getCommonName())
                .notBefore(notBefore)
                .notAfter(notAfter)
                .type(request.getCertificateType())
                .isCa(isCA)
                .certificateData(Base64.getEncoder().encodeToString(signedX509Cert.getEncoded()))
                .owner(requestingUser)
                .issuer(parentCa)
                .createdAt(LocalDateTime.now())
                .build();

        certificateRepository.save(certificate);

        if (isCA) {
            keyStoreService.saveCAKeyStore(serialNumber, newKeyPair.getPrivate(), signedX509Cert, requestingUser);
        }

        log.info("Successfully created {} certificate with serial number: {}",
                request.getCertificateType(), serialNumber);

        return certificateRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new RuntimeException("Failed to retrieve saved certificate"));
    }

    private X509Certificate getX509Certificate(String base64CertData) throws Exception {
        byte[] certBytes = Base64.getDecoder().decode(base64CertData);
        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(certBytes));
    }

    private String generateSerialNumber() {
        return String.valueOf(System.currentTimeMillis());
    }
}