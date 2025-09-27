package com.example.bsep_backend.pki.controller;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.pki.domain.Certificate;
import com.example.bsep_backend.pki.service.AdminService;
import com.example.bsep_backend.security.user.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/root-certificate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createRootCertificate(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody Map<String, String> request) {
        try {
            User user = authUser.getUser();
            String commonName = request.get("commonName");
            Certificate certificate = adminService.createRootCertificate(user, commonName);
            return ResponseEntity.ok(certificate);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating root certificate: " + e.getMessage());
        }
    }

    @GetMapping("/certificates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Certificate>> getAllCertificates() {
        List<Certificate> certificates = adminService.getAllCertificates();
        return ResponseEntity.ok(certificates);
    }
}