package com.example.bsep_backend.pki.repository;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.pki.domain.CSRStatus;
import com.example.bsep_backend.pki.domain.CertificateSigningRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CSRRepository extends JpaRepository<CertificateSigningRequest, Long> {

    @Query("SELECT csr FROM CertificateSigningRequest csr " +
           "JOIN FETCH csr.requester " +
           "LEFT JOIN FETCH csr.reviewer " +
           "LEFT JOIN FETCH csr.selectedCA " +
           "LEFT JOIN FETCH csr.issuedCertificate " +
           "WHERE csr.requester = :requester " +
           "ORDER BY csr.createdAt DESC")
    List<CertificateSigningRequest> findByRequesterOrderByCreatedAtDesc(User requester);

    List<CertificateSigningRequest> findByStatusOrderByCreatedAtDesc(CSRStatus status);

    @Query("SELECT csr FROM CertificateSigningRequest csr " +
           "JOIN FETCH csr.requester " +
           "LEFT JOIN FETCH csr.reviewer " +
           "LEFT JOIN FETCH csr.selectedCA " +
           "LEFT JOIN FETCH csr.issuedCertificate " +
           "ORDER BY csr.createdAt DESC")
    List<CertificateSigningRequest> findAllWithDetails();

    @Query("SELECT csr FROM CertificateSigningRequest csr " +
           "JOIN FETCH csr.requester " +
           "LEFT JOIN FETCH csr.reviewer " +
           "LEFT JOIN FETCH csr.selectedCA " +
           "LEFT JOIN FETCH csr.issuedCertificate " +
           "WHERE csr.status = :status " +
           "ORDER BY csr.createdAt DESC")
    List<CertificateSigningRequest> findByStatusWithDetails(CSRStatus status);
}