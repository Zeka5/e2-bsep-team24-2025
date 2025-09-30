package com.example.bsep_backend.pki.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CAAssignmentRequest {
    @NotNull(message = "CA user ID is required")
    private Long caUserId;

    @NotNull(message = "CA certificate serial number is required")
    private String caCertificateSerialNumber;
}