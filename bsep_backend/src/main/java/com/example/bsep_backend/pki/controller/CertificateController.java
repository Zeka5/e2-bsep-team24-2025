package com.example.bsep_backend.pki.controller;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.pki.domain.Certificate;
import com.example.bsep_backend.pki.dto.CreateCertificateRequest;
import com.example.bsep_backend.pki.service.CertificateService;
import com.example.bsep_backend.security.user.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<List<Certificate>> getAllCertificates() {
        List<Certificate> certificates = certificateService.getAllCertificates();
        return ResponseEntity.ok(certificates);
    }

    @PostMapping("/sign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> signCertificate(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateCertificateRequest request) {
        try {
            User user = authUser.getUser();
            Certificate certificate = certificateService.signCertificate(request, user);
            return ResponseEntity.ok(certificate);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error signing certificate: " + e.getMessage());
        }
    }
}