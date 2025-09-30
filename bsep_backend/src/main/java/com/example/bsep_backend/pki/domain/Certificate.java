package com.example.bsep_backend.pki.domain;

import com.example.bsep_backend.domain.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "certificates")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Certificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String serialNumber;

    @Column(nullable = false)
    private String commonName;

    @Column(nullable = false)
    private LocalDateTime notBefore;

    @Column(nullable = false)
    private LocalDateTime notAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateType type;

    @Column(name = "is_ca")
    @JsonProperty("isCa")
    private boolean isCa = false;

    @Column(columnDefinition = "TEXT")
    private String certificateData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_id")
    private Certificate issuer;

    @Column(nullable = false)
    private String organization;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}