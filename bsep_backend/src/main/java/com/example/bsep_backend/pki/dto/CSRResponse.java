package com.example.bsep_backend.pki.dto;

import com.example.bsep_backend.pki.domain.CSRStatus;
import com.example.bsep_backend.pki.domain.CertificateType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CSRResponse {

    private Long id;
    private String commonName;
    private String organization;
    private String country;
    private CertificateType requestedType;
    private Integer validityDays;
    private CSRStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private String rejectionReason;
    private String requesterEmail;
    private String reviewerEmail;
    private String selectedCaCommonName;
    private String issuedCertificateSerialNumber;
}