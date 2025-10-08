package com.example.bsep_backend.pki.service.impl;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.domain.UserRole;
import com.example.bsep_backend.exception.NotFoundException;
import com.example.bsep_backend.pki.domain.CSRStatus;
import com.example.bsep_backend.pki.domain.Certificate;
import com.example.bsep_backend.pki.domain.CertificateSigningRequest;
import com.example.bsep_backend.pki.dto.CSRResponse;
import com.example.bsep_backend.pki.dto.CreateCSRRequest;
import com.example.bsep_backend.pki.dto.ReviewCSRRequest;
import com.example.bsep_backend.pki.dto.CreateCertificateRequest;
import com.example.bsep_backend.pki.repository.CSRRepository;
import com.example.bsep_backend.pki.repository.CertificateRepository;
import com.example.bsep_backend.pki.service.CSRService;
import com.example.bsep_backend.pki.service.CertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.asn1.x500.X500Name;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CSRServiceImpl implements CSRService {

    private final CSRRepository csrRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateService certificateService;

    @Override
    @Transactional
    public CSRResponse createCSR(CreateCSRRequest request, User requester) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();

            String subjectDN = String.format("CN=%s,O=%s,C=%s",
                    request.getCommonName(), request.getOrganization(), request.getCountry());
            X500Name subject = new X500Name(subjectDN);

            JcaPKCS10CertificationRequestBuilder csrBuilder =
                    new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .build(keyPair.getPrivate());

            PKCS10CertificationRequest csr = csrBuilder.build(signer);
            String csrData = Base64.getEncoder().encodeToString(csr.getEncoded());

            CertificateSigningRequest csrEntity = CertificateSigningRequest.builder()
                    .csrData(csrData)
                    .commonName(request.getCommonName())
                    .organization(request.getOrganization())
                    .country(request.getCountry())
                    .requestedValidityDays(request.getValidityDays())
                    .status(CSRStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .requester(requester)
                    .build();

            CertificateSigningRequest savedCSR = csrRepository.save(csrEntity);

            log.info("CSR created for user {} with common name: {}",
                    requester.getEmail(), request.getCommonName());

            return mapToResponse(savedCSR);

        } catch (Exception e) {
            log.error("Error creating CSR: {}", e.getMessage());
            throw new RuntimeException("Failed to create CSR", e);
        }
    }

    @Override
    public List<CSRResponse> getMyCSRs(User requester) {
        return csrRepository.findByRequesterOrderByCreatedAtDesc(requester)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<CSRResponse> getAllCSRs() {
        return csrRepository.findAllWithDetails()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<CSRResponse> getCSRsByStatus(CSRStatus status) {
        return csrRepository.findByStatusWithDetails(status)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public CSRResponse reviewCSR(Long csrId, ReviewCSRRequest request, User reviewer) {
        CertificateSigningRequest csr = csrRepository.findById(csrId)
                .orElseThrow(() -> new NotFoundException("CSR not found"));

        if (csr.getStatus() != CSRStatus.PENDING) {
            throw new IllegalStateException("CSR has already been reviewed");
        }

        // CA users can only review CSRs from their organization
        if (reviewer.getRole() == UserRole.CA && !csr.getOrganization().equals(reviewer.getOrganization())) {
            throw new IllegalArgumentException("CA users can only review CSRs from their organization");
        }

        csr.setReviewer(reviewer);
        csr.setReviewedAt(LocalDateTime.now());

        if (request.getApproved()) {
            // Validate required fields for approval
            if (request.getSelectedCaSerialNumber() == null) {
                throw new IllegalArgumentException("Selected CA is required when approving CSR");
            }

            csr.setStatus(CSRStatus.APPROVED);

            try {
                Certificate issuedCertificate = createCertificateFromCSR(csr, request, reviewer);
                csr.setIssuedCertificate(issuedCertificate);
                log.info("CSR {} approved and certificate {} issued by {}",
                        csrId, issuedCertificate.getSerialNumber(), reviewer.getEmail());
            } catch (Exception e) {
                log.error("Failed to issue certificate for approved CSR {}: {}", csrId, e.getMessage());
                throw new RuntimeException("Failed to issue certificate from approved CSR", e);
            }
        } else {
            csr.setStatus(CSRStatus.REJECTED);
            csr.setRejectionReason(request.getRejectionReason());
            log.info("CSR {} rejected by {}: {}", csrId, reviewer.getEmail(), request.getRejectionReason());
        }

        CertificateSigningRequest savedCSR = csrRepository.save(csr);
        return mapToResponse(savedCSR);
    }

    @Override
    public CSRResponse getCSRById(Long csrId) {
        CertificateSigningRequest csr = csrRepository.findById(csrId)
                .orElseThrow(() -> new NotFoundException("CSR not found"));
        return mapToResponse(csr);
    }

    @Override
    public List<CSRResponse> getCSRsForUser(User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return getAllCSRs();
        } else if (user.getRole() == UserRole.CA) {
            // CA users can only see CSRs from their organization
            log.info("CA user {} requesting CSRs for organization: {}", user.getEmail(), user.getOrganization());
            if (user.getOrganization() == null) {
                log.error("CA user {} has null organization", user.getEmail());
                throw new IllegalStateException("CA user must have an organization assigned");
            }
            return csrRepository.findByOrganizationWithDetails(user.getOrganization())
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        } else {
            // Regular users can only see their own CSRs
            return getMyCSRs(user);
        }
    }

    @Override
    public List<CSRResponse> getCSRsByStatusForUser(CSRStatus status, User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return getCSRsByStatus(status);
        } else if (user.getRole() == UserRole.CA) {
            // CA users can only see CSRs from their organization with specific status
            return csrRepository.findByOrganizationAndStatusWithDetails(user.getOrganization(), status)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        } else {
            // Regular users can only see their own CSRs with specific status
            return csrRepository.findByRequesterOrderByCreatedAtDesc(user)
                    .stream()
                    .filter(csr -> csr.getStatus() == status)
                    .map(this::mapToResponse)
                    .toList();
        }
    }

    private CSRResponse mapToResponse(CertificateSigningRequest csr) {
        return CSRResponse.builder()
                .id(csr.getId())
                .commonName(csr.getCommonName())
                .organization(csr.getOrganization())
                .country(csr.getCountry())
                .requestedType(null) // No longer stored in CSR - CA decides during review
                .validityDays(csr.getRequestedValidityDays())
                .status(csr.getStatus())
                .createdAt(csr.getCreatedAt())
                .reviewedAt(csr.getReviewedAt())
                .rejectionReason(csr.getRejectionReason())
                .requesterEmail(csr.getRequester().getEmail())
                .reviewerEmail(csr.getReviewer() != null ? csr.getReviewer().getEmail() : null)
                .selectedCaCommonName(null) // No longer stored in CSR - CA decides during review
                .issuedCertificateSerialNumber(csr.getIssuedCertificate() != null ?
                        csr.getIssuedCertificate().getSerialNumber() : null)
                .build();
    }

    private Certificate createCertificateFromCSR(CertificateSigningRequest csr, ReviewCSRRequest reviewRequest, User reviewer) throws Exception {
        // Validate the selected CA
        Certificate selectedCA = certificateRepository.findBySerialNumber(reviewRequest.getSelectedCaSerialNumber())
                .orElseThrow(() -> new NotFoundException("Selected CA certificate not found"));

        if (!selectedCA.isCa()) {
            throw new IllegalArgumentException("Selected certificate is not a CA");
        }

        CreateCertificateRequest certRequest = new CreateCertificateRequest();
        certRequest.setCommonName(csr.getCommonName());
        certRequest.setOrganization(csr.getOrganization());
        certRequest.setCountry(csr.getCountry());
        certRequest.setCertificateType(com.example.bsep_backend.pki.domain.CertificateType.END_ENTITY);
        certRequest.setValidityDays(csr.getRequestedValidityDays());
        certRequest.setParentCaSerialNumber(reviewRequest.getSelectedCaSerialNumber());

        return certificateService.signCertificate(certRequest, csr.getRequester()); // Certificate belongs to the original requester
    }
}