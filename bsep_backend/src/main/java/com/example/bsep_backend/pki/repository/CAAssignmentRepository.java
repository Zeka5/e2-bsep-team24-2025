package com.example.bsep_backend.pki.repository;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.pki.domain.CAAssignment;
import com.example.bsep_backend.pki.domain.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CAAssignmentRepository extends JpaRepository<CAAssignment, Long> {

    @Query("SELECT ca FROM CAAssignment ca LEFT JOIN FETCH ca.caUser LEFT JOIN FETCH ca.caCertificate LEFT JOIN FETCH ca.assignedBy WHERE ca.caUser = :user AND ca.isActive = true")
    List<CAAssignment> findActiveAssignmentsByUser(@Param("user") User user);

    @Query("SELECT ca FROM CAAssignment ca LEFT JOIN FETCH ca.caUser LEFT JOIN FETCH ca.caCertificate WHERE ca.organization = :organization AND ca.isActive = true")
    List<CAAssignment> findActiveAssignmentsByOrganization(@Param("organization") String organization);

    @Query("SELECT ca FROM CAAssignment ca LEFT JOIN FETCH ca.caUser LEFT JOIN FETCH ca.caCertificate WHERE ca.caCertificate = :certificate AND ca.isActive = true")
    List<CAAssignment> findActiveAssignmentsByCertificate(@Param("certificate") Certificate certificate);

    @Query("SELECT ca FROM CAAssignment ca LEFT JOIN FETCH ca.caUser LEFT JOIN FETCH ca.caCertificate LEFT JOIN FETCH ca.assignedBy")
    List<CAAssignment> findAllWithDetails();

    Optional<CAAssignment> findByCaUserAndCaCertificateAndIsActiveTrue(User caUser, Certificate caCertificate);

    boolean existsByCaUserAndCaCertificateAndIsActiveTrue(User caUser, Certificate caCertificate);
}