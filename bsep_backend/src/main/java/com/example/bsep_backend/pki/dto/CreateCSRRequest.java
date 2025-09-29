package com.example.bsep_backend.pki.dto;

import com.example.bsep_backend.pki.domain.CertificateType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCSRRequest {

    @NotBlank(message = "Common name is required")
    @Size(max = 100, message = "Common name must not exceed 100 characters")
    private String commonName;

    @NotBlank(message = "Organization is required")
    @Size(max = 100, message = "Organization must not exceed 100 characters")
    private String organization;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 2, message = "Country must be 2 characters")
    private String country;

    @NotNull(message = "Certificate type is required")
    private CertificateType requestedType;

    @NotNull(message = "Validity days is required")
    @Min(value = 1, message = "Validity days must be positive")
    private Integer validityDays;

    @NotNull(message = "Selected CA is required")
    private String selectedCaSerialNumber;
}