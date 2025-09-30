package com.example.bsep_backend.pki.repository;

import com.example.bsep_backend.pki.domain.Certificate;
import com.example.bsep_backend.pki.domain.CertificateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    @Query("SELECT c FROM Certificate c LEFT JOIN FETCH c.owner LEFT JOIN FETCH c.issuer WHERE c.serialNumber = :serialNumber")
    Optional<Certificate> findBySerialNumber(String serialNumber);
    List<Certificate> findByOwnerIdAndType(Long ownerId, CertificateType type);
    List<Certificate> findByType(CertificateType type);
    List<Certificate> findByIsCaTrue();

    @Query("SELECT c FROM Certificate c LEFT JOIN FETCH c.owner LEFT JOIN FETCH c.issuer")
    List<Certificate> findAllWithOwnerAndIssuer();

    @Query("SELECT c FROM Certificate c LEFT JOIN FETCH c.owner LEFT JOIN FETCH c.issuer WHERE c.organization = :organization")
    List<Certificate> findByOrganizationWithOwnerAndIssuer(@Param("organization") String organization);

    @Query("SELECT c FROM Certificate c LEFT JOIN FETCH c.owner WHERE c.organization = :organization AND c.isCa = true")
    List<Certificate> findCACertificatesByOrganization(@Param("organization") String organization);

    @Query("SELECT c FROM Certificate c LEFT JOIN FETCH c.owner LEFT JOIN FETCH c.issuer WHERE c.issuer IN :parentCertificates")
    List<Certificate> findByIssuerIn(@Param("parentCertificates") List<Certificate> parentCertificates);
}