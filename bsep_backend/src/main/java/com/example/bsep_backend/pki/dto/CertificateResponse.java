package com.example.bsep_backend.pki.dto;

import com.example.bsep_backend.pki.domain.CertificateType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CertificateResponse {
    private Long id;
    private String serialNumber;
    private String commonName;
    private CertificateType type;
    private boolean isCa;
    private LocalDateTime notBefore;
    private LocalDateTime notAfter;
    private String issuerCommonName;
    private String ownerName;
    private String certificateData; // Base64 encoded certificate
}