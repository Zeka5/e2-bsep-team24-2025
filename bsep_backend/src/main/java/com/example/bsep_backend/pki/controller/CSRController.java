package com.example.bsep_backend.pki.controller;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.pki.domain.CSRStatus;
import com.example.bsep_backend.pki.dto.CSRResponse;
import com.example.bsep_backend.pki.dto.CreateCSRRequest;
import com.example.bsep_backend.pki.dto.ReviewCSRRequest;
import com.example.bsep_backend.pki.service.CSRService;
import com.example.bsep_backend.security.user.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/csr")
@RequiredArgsConstructor
public class CSRController {

    private final CSRService csrService;

    @PostMapping
    public ResponseEntity<?> createCSR(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateCSRRequest request) {
        try {
            User user = authUser.getUser();
            CSRResponse response = csrService.createCSR(request, user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating CSR: " + e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<List<CSRResponse>> getMyCSRs(@AuthenticationPrincipal AuthUser authUser) {
        User user = authUser.getUser();
        List<CSRResponse> csrs = csrService.getMyCSRs(user);
        return ResponseEntity.ok(csrs);
    }

    @GetMapping
    @PreAuthorize("hasRole('CA') or hasRole('ADMIN')")
    public ResponseEntity<List<CSRResponse>> getAllCSRs() {
        List<CSRResponse> csrs = csrService.getAllCSRs();
        return ResponseEntity.ok(csrs);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('CA') or hasRole('ADMIN')")
    public ResponseEntity<List<CSRResponse>> getCSRsByStatus(@PathVariable CSRStatus status) {
        List<CSRResponse> csrs = csrService.getCSRsByStatus(status);
        return ResponseEntity.ok(csrs);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CA') or hasRole('ADMIN')")
    public ResponseEntity<CSRResponse> getCSRById(@PathVariable Long id) {
        CSRResponse csr = csrService.getCSRById(id);
        return ResponseEntity.ok(csr);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('CA')")
    public ResponseEntity<?> reviewCSR(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ReviewCSRRequest request) {
        try {
            User reviewer = authUser.getUser();
            CSRResponse response = csrService.reviewCSR(id, request, reviewer);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error reviewing CSR: " + e.getMessage());
        }
    }
}