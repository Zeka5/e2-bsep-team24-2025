package com.example.bsep_backend.pki.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CAAssignmentResponse {
    private Long id;
    private Long caUserId;
    private String caUserEmail;
    private String caUserName;
    private String caCertificateSerialNumber;
    private String caCertificateCommonName;
    private String organization;
    private LocalDateTime assignedAt;
    private String assignedByEmail;
    private boolean isActive;
}