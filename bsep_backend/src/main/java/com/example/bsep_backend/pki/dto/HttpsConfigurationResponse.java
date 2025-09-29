package com.example.bsep_backend.pki.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HttpsConfigurationResponse {
    private String applicationProperties;
    private byte[] keystoreFile;
    private String keystoreFilename;
    private String certificateInfo;
    private String[] subjectAlternativeNames;
    private String commonName;
    private String validityPeriod;
}