package com.example.bsep_backend.pki.repository;

import com.example.bsep_backend.pki.domain.Certificate;
import com.example.bsep_backend.pki.domain.CertificateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findBySerialNumber(String serialNumber);
    List<Certificate> findByOwnerIdAndType(Long ownerId, CertificateType type);
    List<Certificate> findByType(CertificateType type);
    List<Certificate> findByIsCaTrue();
}