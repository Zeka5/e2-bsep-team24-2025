package com.example.bsep_backend.pki.dto;

import com.example.bsep_backend.pki.domain.CertificateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class CreateCertificateRequest {

    @NotBlank(message = "Common name is required")
    private String commonName;

    @NotBlank(message = "Organization is required")
    private String organization;

    @NotBlank(message = "Country is required")
    private String country;

    private String organizationUnit;
    private String locality;
    private String state;

    @NotNull(message = "Certificate type is required")
    private CertificateType certificateType;

    @NotBlank(message = "Parent CA serial number is required")
    private String parentCaSerialNumber;

    @Positive(message = "Validity days must be positive")
    private int validityDays = 365;

    private List<String> subjectAlternativeNames;

    // For intermediate CAs
    private Integer pathLengthConstraint;

    // Key usage extensions
    private boolean keyEncipherment = false;
    private boolean dataEncipherment = false;
    private boolean keyAgreement = false;
    private boolean keyCertSign = false;
    private boolean crlSign = false;
    private boolean digitalSignature = true;
    private boolean nonRepudiation = false;
}