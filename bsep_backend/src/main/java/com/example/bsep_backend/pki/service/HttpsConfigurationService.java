package com.example.bsep_backend.pki.service;

import com.example.bsep_backend.pki.dto.HttpsConfigurationResponse;

public interface HttpsConfigurationService {

    HttpsConfigurationResponse generateSpringBootSslConfig(String certificateSerialNumber, String keystorePassword);

    String generateApplicationProperties(String certificateSerialNumber, String keystorePassword, int port);

    void exportHttpsConfigurationBundle(String certificateSerialNumber, String keystorePassword, String outputPath) throws Exception;
}