package com.example.bsep_backend.pki.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewCSRRequest {

    @NotNull(message = "Approval decision is required")
    private Boolean approved;

    @NotBlank(message = "Rejection reason is required when rejecting", groups = RejectionGroup.class)
    private String rejectionReason;

    public interface RejectionGroup {}
}