package com.example.bsep_backend.pki.dto;

import com.example.bsep_backend.pki.domain.CertificateType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CertificateResponse {
    private Long id;
    private String serialNumber;
    private String commonName;
    private String organization;
    private CertificateType type;

    @JsonProperty("isCa")
    private boolean isCa;

    private LocalDateTime notBefore;
    private LocalDateTime notAfter;
    private LocalDateTime createdAt;

    // Owner information
    private Long ownerId;
    private String ownerEmail;
    private String ownerName;

    // Issuer information (parent certificate)
    private String issuerSerialNumber;
    private String issuerCommonName;

    // Optional: Certificate data for download (only when specifically requested)
    private String certificateData;
}