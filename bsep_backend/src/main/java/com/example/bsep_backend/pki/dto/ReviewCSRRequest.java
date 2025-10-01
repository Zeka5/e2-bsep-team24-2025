package com.example.bsep_backend.pki.dto;

import com.example.bsep_backend.pki.domain.CertificateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewCSRRequest {

    @NotNull(message = "Approval decision is required")
    private Boolean approved;

    @NotBlank(message = "Rejection reason is required when rejecting", groups = RejectionGroup.class)
    private String rejectionReason;

    private String selectedCaSerialNumber; // CA chooses which CA to use for signing

    public interface RejectionGroup {}
}