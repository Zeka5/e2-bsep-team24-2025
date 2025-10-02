package com.example.bsep_backend.pki.controller;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.pki.domain.Certificate;
import com.example.bsep_backend.pki.dto.CreateCertificateRequest;
import com.example.bsep_backend.pki.dto.CertificateResponse;
import com.example.bsep_backend.pki.dto.CertificateExportResponse;
import com.example.bsep_backend.pki.service.CertificateExportService;
import com.example.bsep_backend.pki.service.CertificateService;
import com.example.bsep_backend.pki.service.HttpsConfigurationService;
import com.example.bsep_backend.pki.dto.HttpsConfigurationResponse;
import com.example.bsep_backend.security.user.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;
    private final CertificateExportService certificateExportService;
    private final HttpsConfigurationService httpsConfigurationService;

    @PostMapping("/root")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createRootCertificate(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody Map<String, String> request) {
        try {
            User user = authUser.getUser();
            String commonName = request.get("commonName");
            Certificate certificate = certificateService.createRootCertificate(user, commonName);
            return ResponseEntity.ok(certificate);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating root certificate: " + e.getMessage());
        }
    }

    @GetMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CertificateResponse>> getAllCertificates() {
        List<CertificateResponse> certificates = certificateService.getAllCertificateResponses();
        return ResponseEntity.ok(certificates);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CA') or hasRole('USER')")
    public ResponseEntity<List<CertificateResponse>> getMyCertificates(@AuthenticationPrincipal AuthUser authUser) {
        User user = authUser.getUser();
        List<CertificateResponse> certificates = certificateService.getCertificateResponsesForUser(user);
        return ResponseEntity.ok(certificates);
    }

    @GetMapping("/available-parent-cas")
    @PreAuthorize("hasRole('CA') or hasRole('ADMIN')")
    public ResponseEntity<List<CertificateResponse>> getAvailableParentCAs(@AuthenticationPrincipal AuthUser authUser) {
        User user = authUser.getUser();
        List<CertificateResponse> availableCAs = certificateService.getAvailableParentCAResponses(user);
        return ResponseEntity.ok(availableCAs);
    }

    @GetMapping("/ca-certificates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CertificateResponse>> getCACertificates() {
        List<CertificateResponse> caCertificates = certificateService.getAllCertificateResponses().stream()
                .filter(cert -> cert.isCa())
                .toList();
        return ResponseEntity.ok(caCertificates);
    }

    @PostMapping("/sign")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CA')")
    public ResponseEntity<?> signCertificate(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateCertificateRequest request) {
        try {
            User user = authUser.getUser();
            CertificateResponse certificate = certificateService.signCertificateResponse(request, user);
            return ResponseEntity.ok(certificate);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error signing certificate: " + e.getMessage());
        }
    }

    @GetMapping("/{serialNumber}/export")
    public ResponseEntity<byte[]> exportCertificate(
            @PathVariable String serialNumber,
            @RequestParam(defaultValue = "pem") String format) {
        try {
            CertificateExportResponse exportResponse = certificateExportService.exportCertificate(serialNumber, format);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportResponse.getFilename() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, exportResponse.getContentType());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(exportResponse.getContent());

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{serialNumber}/keystore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportKeystore(
            @PathVariable String serialNumber,
            @RequestParam String password) {
        try {
            CertificateExportResponse exportResponse = certificateExportService.exportKeystore(serialNumber, password);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportResponse.getFilename() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, exportResponse.getContentType());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(exportResponse.getContent());

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{serialNumber}/https-config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HttpsConfigurationResponse> getHttpsConfiguration(
            @PathVariable String serialNumber,
            @RequestParam String keystorePassword) {
        try {
            HttpsConfigurationResponse config = httpsConfigurationService.generateSpringBootSslConfig(serialNumber, keystorePassword);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{serialNumber}/application-properties")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getApplicationProperties(
            @PathVariable String serialNumber,
            @RequestParam String keystorePassword,
            @RequestParam(defaultValue = "8443") int port) {
        try {
            String properties = httpsConfigurationService.generateApplicationProperties(serialNumber, keystorePassword, port);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"application-https.properties\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "text/plain");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(properties);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}