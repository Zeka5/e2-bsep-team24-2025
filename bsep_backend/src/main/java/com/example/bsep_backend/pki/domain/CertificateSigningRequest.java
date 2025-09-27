package com.example.bsep_backend.pki.domain;

import com.example.bsep_backend.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "certificate_signing_requests")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CertificateSigningRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String csrData;

    @Column(nullable = false)
    private String commonName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CSRStatus status = CSRStatus.PENDING;

    @Column(name = "requested_validity_days")
    private Integer requestedValidityDays;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_ca_id")
    private Certificate selectedCA;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}