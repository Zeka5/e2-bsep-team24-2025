package com.example.bsep_backend.pki.service;

import com.example.bsep_backend.pki.dto.CertificateExportResponse;

public interface CertificateExportService {

    CertificateExportResponse exportCertificate(String serialNumber, String format);

    CertificateExportResponse exportKeystore(String serialNumber, String keystorePassword);
}