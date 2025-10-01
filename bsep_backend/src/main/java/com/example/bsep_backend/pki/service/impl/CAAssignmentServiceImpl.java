package com.example.bsep_backend.pki.service.impl;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.domain.UserRole;
import com.example.bsep_backend.exception.NotFoundException;
import com.example.bsep_backend.pki.domain.CAAssignment;
import com.example.bsep_backend.pki.domain.Certificate;
import com.example.bsep_backend.pki.dto.CAAssignmentRequest;
import com.example.bsep_backend.pki.dto.CAAssignmentResponse;
import com.example.bsep_backend.pki.repository.CAAssignmentRepository;
import com.example.bsep_backend.pki.repository.CertificateRepository;
import com.example.bsep_backend.pki.service.CAAssignmentService;
import com.example.bsep_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CAAssignmentServiceImpl implements CAAssignmentService {

    private final CAAssignmentRepository caAssignmentRepository;
    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CAAssignmentResponse assignCAToCaUser(CAAssignmentRequest request, User admin) {
        User caUser = userRepository.findById(request.getCaUserId())
                .orElseThrow(() -> new NotFoundException("CA user not found"));

        if (caUser.getRole() != UserRole.CA) {
            throw new IllegalArgumentException("User must have CA role to be assigned a CA certificate");
        }

        Certificate caCertificate = certificateRepository.findBySerialNumber(request.getCaCertificateSerialNumber())
                .orElseThrow(() -> new NotFoundException("CA certificate not found"));

        if (!caCertificate.isCa()) {
            throw new IllegalArgumentException("Certificate must be a CA certificate");
        }

        // Validate organization match
        if (!caUser.getOrganization().equals(caCertificate.getOrganization())) {
            log.info("users org: {}, certificates org: {}",caUser.getOrganization(), caCertificate.getOrganization());
            throw new IllegalArgumentException("CA user and certificate must belong to the same organization");
        }

        // Check if assignment already exists
        if (caAssignmentRepository.existsByCaUserAndCaCertificateAndIsActiveTrue(caUser, caCertificate)) {
            throw new IllegalArgumentException("CA certificate is already assigned to this user");
        }

        CAAssignment assignment = CAAssignment.builder()
                .caUser(caUser)
                .caCertificate(caCertificate)
                .organization(caUser.getOrganization())
                .assignedBy(admin)
                .isActive(true)
                .build();

        CAAssignment savedAssignment = caAssignmentRepository.save(assignment);

        log.info("CA certificate {} assigned to user {} by admin {}",
                caCertificate.getCommonName(), caUser.getEmail(), admin.getEmail());

        return mapToResponse(savedAssignment);
    }

    @Override
    @Transactional
    public void revokeCAAssignment(Long assignmentId, User admin) {
        CAAssignment assignment = caAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("CA assignment not found"));

        assignment.setActive(false);
        caAssignmentRepository.save(assignment);

        log.info("CA assignment {} revoked by admin {}", assignmentId, admin.getEmail());
    }

    @Override
    public List<CAAssignmentResponse> getAllAssignments() {
        return caAssignmentRepository.findAllWithDetails().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CAAssignmentResponse> getAssignmentsByOrganization(String organization) {
        return caAssignmentRepository.findActiveAssignmentsByOrganization(organization).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<Certificate> getAssignedCertificatesForUser(User caUser) {
        return caAssignmentRepository.findActiveAssignmentsByUser(caUser).stream()
                .map(CAAssignment::getCaCertificate)
                .collect(Collectors.toList());
    }

    @Override
    public boolean canUserUseCertificate(User caUser, String certificateSerialNumber) {
        if (caUser.getRole() != UserRole.CA) {
            return false;
        }

        Certificate certificate = certificateRepository.findBySerialNumber(certificateSerialNumber)
                .orElse(null);

        if (certificate == null || !certificate.isCa()) {
            return false; // Only CA certificates can be used for signing
        }

        // Check if certificate is in user's complete chain (not just assigned)
        List<Certificate> userChainCertificates = getCertificatesInUserChain(caUser);
        return userChainCertificates.stream()
                .filter(Certificate::isCa) // Only CA certificates can be used for signing
                .anyMatch(cert -> cert.getSerialNumber().equals(certificateSerialNumber));
    }

    @Override
    public List<Certificate> getCertificatesInUserChain(User caUser) {
        // Start with certificates assigned to the CA user
        List<Certificate> assignedCertificates = getAssignedCertificatesForUser(caUser);
        log.info("Signed certificate number: {}", assignedCertificates.size());

        // Build the complete chain recursively
        Set<Certificate> chainCertificates = new HashSet<>();
        buildCertificateChain(assignedCertificates, chainCertificates);
        log.info("Chian number: {}", chainCertificates.size());

        return new ArrayList<>(chainCertificates);
    }

    private void buildCertificateChain(List<Certificate> parentCertificates, Set<Certificate> result) {
        if (parentCertificates.isEmpty()) {
            return;
        }

        // Add current level certificates to result
        result.addAll(parentCertificates);

        // Find all certificates issued by current level certificates
        List<Certificate> childCertificates = certificateRepository.findByIssuerIn(parentCertificates);

        // Remove already processed certificates to avoid infinite loops
        childCertificates.removeAll(result);

        // Recursively process child certificates
        if (!childCertificates.isEmpty()) {
            buildCertificateChain(childCertificates, result);
        }
    }

    @Override
    public List<CAAssignmentResponse> getAssignmentsForUser(User caUser) {
        return caAssignmentRepository.findActiveAssignmentsByUser(caUser).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private CAAssignmentResponse mapToResponse(CAAssignment assignment) {
        return CAAssignmentResponse.builder()
                .id(assignment.getId())
                .caUserId(assignment.getCaUser().getId())
                .caUserEmail(assignment.getCaUser().getEmail())
                .caUserName(assignment.getCaUser().getName() + " " + assignment.getCaUser().getSurname())
                .caCertificateSerialNumber(assignment.getCaCertificate().getSerialNumber())
                .caCertificateCommonName(assignment.getCaCertificate().getCommonName())
                .organization(assignment.getOrganization())
                .assignedAt(assignment.getAssignedAt())
                .assignedByEmail(assignment.getAssignedBy().getEmail())
                .isActive(assignment.isActive())
                .build();
    }
}