package com.example.bsep_backend.pki.service;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.domain.UserRole;
import com.example.bsep_backend.exception.NotFoundException;
import com.example.bsep_backend.pki.domain.*;
import com.example.bsep_backend.pki.dto.CreateCertificateRequest;
import com.example.bsep_backend.pki.dto.CertificateResponse;
import com.example.bsep_backend.pki.repository.CertificateRepository;
import com.example.bsep_backend.pki.service.CAAssignmentService;
import com.example.bsep_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateGenerator certificateGenerator;
    private final KeyStoreService keyStoreService;
    private final CAAssignmentService caAssignmentService;
    private final UserRepository userRepository;

    public Certificate createRootCertificate(User admin, String commonName) throws Exception {
        KeyPair keyPair = certificateGenerator.generateKeyPair();
        LocalDateTime notBefore = LocalDateTime.now();
        LocalDateTime notAfter = notBefore.plusYears(10);
        String serialNumber = generateSerialNumber();

        String subjectDN = String.format("CN=%s,O=PKI Organization,C=RS", commonName);
        X500Name x500Name = new X500Name(subjectDN);

        Subject subject = new Subject(keyPair.getPublic(), x500Name);
        Issuer issuer = new Issuer(keyPair.getPrivate(), keyPair.getPublic(), x500Name);

        // Root CA is self-signed and must be a CA certificate
        X509Certificate x509Certificate = certificateGenerator.generateCertificateWithSAN(
                subject, issuer, notBefore, notAfter, serialNumber, null, true
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
                .organization(admin.getOrganization())
                .createdAt(LocalDateTime.now())
                .build();

        Certificate savedCertificate = certificateRepository.save(certificate);

        keyStoreService.saveCAKeyStore(serialNumber, keyPair.getPrivate(), x509Certificate, admin);

        return savedCertificate;
    }
    public java.util.List<Certificate> getAllCertificates() {
        return certificateRepository.findAllWithOwnerAndIssuer();
    }

    public java.util.List<Certificate> getCertificatesForUser(User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return certificateRepository.findAllWithOwnerAndIssuer();
        } else if (user.getRole() == UserRole.CA) {
            log.info("Getting certificates for CA user: {} (organization: {})", user.getEmail(), user.getOrganization());
            List<Certificate> certs = getCertificatesInUserChain(user);
            log.info("Found {} certificates", certs.size());
            return certs;
        } else {
            log.info("User with id {} requests his certificates", user.getId());
            return certificateRepository.findByOwnerId(user.getId());
        }
    }

    private java.util.List<Certificate> getCertificatesInUserChain(User caUser) {
        return caAssignmentService.getCertificatesInUserChain(caUser);
    }

    public java.util.List<Certificate> getAvailableParentCAs(User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return certificateRepository.findByIsCaTrue();
        } else if (user.getRole() == UserRole.CA) {
            // CA users can only use CA certificates from their assigned chain
            return caAssignmentService.getAssignedCertificatesForUser(user);
        } else {
            return java.util.Collections.emptyList();
        }
    }

    public CertificateResponse signCertificateResponse(CreateCertificateRequest request, User requestingUser) throws Exception {
        Certificate certificate = signCertificate(request, requestingUser);
        return mapToCertificateResponse(certificate);
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

        // Chain-based validation for CA users
        if (requestingUser.getRole() == UserRole.CA) {
            boolean canUseParentCA = caAssignmentService.canUserUseCertificate(requestingUser, parentCa.getSerialNumber());
            if (!canUseParentCA) {
                throw new IllegalArgumentException("CA users can only use certificates from their assigned chain");
            }
            if (!request.getOrganization().equals(requestingUser.getOrganization())) {
                throw new IllegalArgumentException("CA users can only issue certificates for their organization");
            }
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
                .organization(request.getOrganization())
                .createdAt(LocalDateTime.now())
                .build();

        certificateRepository.save(certificate);

        if (isCA) {
            keyStoreService.saveCAKeyStore(serialNumber, newKeyPair.getPrivate(), signedX509Cert, requestingUser);
        } else {
            // For End-Entity certificates, store only the certificate (no private key)
            keyStoreService.saveEECertificate(serialNumber, signedX509Cert, requestingUser);
        }

        log.info("Successfully created {} certificate with serial number: {}",
                request.getCertificateType(), serialNumber);

        return certificateRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new RuntimeException("Failed to retrieve saved certificate"));
    }

    public Certificate signCertificateFromCSR(CertificateSigningRequest csr, Certificate parentCa, User caUser) throws Exception {
        if (!parentCa.isCa()) {
            throw new IllegalArgumentException("Parent certificate is not a CA certificate");
        }

        if (parentCa.getNotAfter().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Parent CA certificate has expired");
        }

        if (caUser.getRole() == UserRole.CA) {
            boolean canUseParentCA = caAssignmentService.canUserUseCertificate(caUser, parentCa.getSerialNumber());
            if (!canUseParentCA) {
                throw new IllegalArgumentException("CA users can only use certificates from their assigned chain");
            }
            if (!csr.getOrganization().equals(caUser.getOrganization())) {
                throw new IllegalArgumentException("CA users can only issue certificates for their organization");
            }
        }

        PrivateKey parentPrivateKey = keyStoreService.getPrivateKey(parentCa.getSerialNumber());
        X509Certificate parentX509Cert = getX509Certificate(parentCa.getCertificateData());

        byte[] csrBytes = Base64.getDecoder().decode(csr.getCsrData());
        PKCS10CertificationRequest pkcs10Request = new PKCS10CertificationRequest(csrBytes);

        boolean isValid = pkcs10Request.isSignatureValid(
                new JcaContentVerifierProviderBuilder().build(pkcs10Request.getSubjectPublicKeyInfo()));
        if (!isValid) {
            throw new IllegalArgumentException("CSR signature is invalid");
        }

        PublicKey requesterPublicKey = new JcaPEMKeyConverter()
                .setProvider("BC")
                .getPublicKey(pkcs10Request.getSubjectPublicKeyInfo());
        log.info("=== CSR Public Key Details ===");
        log.info("Algorithm: {}, Format: {}", requesterPublicKey.getAlgorithm(), requesterPublicKey.getFormat());
        log.info("Public Key: {}", requesterPublicKey);
        log.info("==============================");
        X500Name subjectX500Name = pkcs10Request.getSubject();

        LocalDateTime notBefore = LocalDateTime.now();
        LocalDateTime notAfter = notBefore.plusDays(csr.getRequestedValidityDays());
        if (notAfter.isAfter(parentCa.getNotAfter())) {
            notAfter = parentCa.getNotAfter();
        }

        String serialNumber = generateSerialNumber();
        boolean isCA = false; // end-entity

        X500Name issuerX500Name = new X500Name(parentX509Cert.getSubjectDN().getName());

        Subject subject = new Subject(requesterPublicKey, subjectX500Name);
        Issuer issuer = new Issuer(parentPrivateKey, parentX509Cert.getPublicKey(), issuerX500Name);

        //if any
//        List<String> sans = new ArrayList<>();
//        try {
//            Extensions extensions = (Extensions) pkcs10Request.getAttributes()[0].getAttrValues().getObjectAt(0);
//            GeneralNames gns = GeneralNames.fromExtensions(extensions, org.bouncycastle.asn1.x509.Extension.subjectAlternativeName);
//            if (gns != null) {
//                for (GeneralName gn : gns.getNames()) {
//                    sans.add(gn.getName().toString());
//                }
//            }
//        } catch (Exception ignored) {}

        List<String> sans = List.of("DNS:localhost", "IP:127.0.0.1");

        X509Certificate signedX509Cert = certificateGenerator.generateCertificateWithSAN(
                subject, issuer, notBefore, notAfter, serialNumber, sans, isCA);

        // Build database entity
        Certificate certificate = Certificate.builder()
                .serialNumber(serialNumber)
                .commonName(csr.getCommonName())
                .notBefore(notBefore)
                .notAfter(notAfter)
                .type(CertificateType.END_ENTITY)
                .isCa(isCA)
                .certificateData(Base64.getEncoder().encodeToString(signedX509Cert.getEncoded()))
                .owner(csr.getRequester())
                .issuer(parentCa)
                .organization(csr.getOrganization())
                .createdAt(LocalDateTime.now())
                .build();

        certificateRepository.save(certificate);

        keyStoreService.saveEECertificate(serialNumber, signedX509Cert, csr.getRequester());

        log.info("Issued end-entity certificate {} from CSR {}", serialNumber, csr.getId());

        return certificate;
    }


    private X509Certificate getX509Certificate(String base64CertData) throws Exception {
        byte[] certBytes = Base64.getDecoder().decode(base64CertData);
        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(certBytes));
    }

    public java.security.cert.Certificate getCertificateFromKeystore(String serialNumber) throws Exception {
        Certificate dbCertificate = certificateRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new NotFoundException("Certificate not found"));

        if (dbCertificate.isCa()) {
            // For CA certificates, get from CA keystore
            KeyStore keyStore = keyStoreService.loadKeyStore(serialNumber);
            return keyStore.getCertificate(serialNumber);
        } else {
            // For EE certificates, get from EE keystore
            return keyStoreService.getEECertificate(serialNumber);
        }
    }

    public List<CertificateResponse> getCertificateResponsesForUser(User user) {
        return getCertificatesForUser(user).stream()
                .map(this::mapToCertificateResponse)
                .toList();
    }

    public List<CertificateResponse> getCertificateResponsesForUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return certificateRepository.findByOwnerId(userId).stream()
                .map(this::mapToCertificateResponse)
                .toList();
    }

    public List<CertificateResponse> getAllCertificateResponses() {
        return getAllCertificates().stream()
                .map(this::mapToCertificateResponse)
                .toList();
    }

    public List<CertificateResponse> getAvailableParentCAResponses(User user) {
        return getAvailableParentCAs(user).stream()
                .map(this::mapToCertificateResponse)
                .toList();
    }

    private CertificateResponse mapToCertificateResponse(Certificate certificate) {
        return CertificateResponse.builder()
                .id(certificate.getId())
                .serialNumber(certificate.getSerialNumber())
                .commonName(certificate.getCommonName())
                .organization(certificate.getOrganization())
                .type(certificate.getType())
                .isCa(certificate.isCa())
                .notBefore(certificate.getNotBefore())
                .notAfter(certificate.getNotAfter())
                .createdAt(certificate.getCreatedAt())
                .ownerId(certificate.getOwner() != null ? certificate.getOwner().getId() : null)
                .ownerEmail(certificate.getOwner() != null ? certificate.getOwner().getEmail() : null)
                .ownerName(certificate.getOwner() != null ?
                    certificate.getOwner().getName() + " " + certificate.getOwner().getSurname() : null)
                .issuerSerialNumber(certificate.getIssuer() != null ? certificate.getIssuer().getSerialNumber() : null)
                .issuerCommonName(certificate.getIssuer() != null ? certificate.getIssuer().getCommonName() : null)
                .certificateData(null) // Don't include certificate data by default (only for specific download requests)
                .build();
    }

    private String generateSerialNumber() {
        return String.valueOf(System.currentTimeMillis());
    }
}