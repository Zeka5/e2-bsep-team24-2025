package com.example.bsep_backend.pki.repository;

import com.example.bsep_backend.pki.domain.CertificateSigningRequest;
import com.example.bsep_backend.pki.domain.CSRStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificateSigningRequestRepository extends JpaRepository<CertificateSigningRequest, Long> {
    List<CertificateSigningRequest> findByRequesterId(Long requesterId);
    List<CertificateSigningRequest> findByStatus(CSRStatus status);
}