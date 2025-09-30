package com.example.bsep_backend.pki.service;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.pki.domain.Certificate;
import com.example.bsep_backend.pki.dto.CAAssignmentRequest;
import com.example.bsep_backend.pki.dto.CAAssignmentResponse;

import java.util.List;

public interface CAAssignmentService {
    CAAssignmentResponse assignCAToCaUser(CAAssignmentRequest request, User admin);

    void revokeCAAssignment(Long assignmentId, User admin);

    List<CAAssignmentResponse> getAllAssignments();

    List<CAAssignmentResponse> getAssignmentsByOrganization(String organization);

    List<Certificate> getAssignedCertificatesForUser(User caUser);

    boolean canUserUseCertificate(User caUser, String certificateSerialNumber);

    List<CAAssignmentResponse> getAssignmentsForUser(User caUser);

    List<Certificate> getCertificatesInUserChain(User caUser);
}