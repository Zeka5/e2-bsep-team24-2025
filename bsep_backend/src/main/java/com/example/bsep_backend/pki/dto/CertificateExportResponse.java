package com.example.bsep_backend.pki.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CertificateExportResponse {

    private String filename;
    private String contentType;
    private byte[] content;
    private String format;
}