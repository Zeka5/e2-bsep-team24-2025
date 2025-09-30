package com.example.bsep_backend.pki.controller;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.pki.dto.CAAssignmentRequest;
import com.example.bsep_backend.pki.dto.CAAssignmentResponse;
import com.example.bsep_backend.pki.service.CAAssignmentService;
import com.example.bsep_backend.security.user.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ca-assignments")
@RequiredArgsConstructor
public class CAAssignmentController {

    private final CAAssignmentService caAssignmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignCAToCaUser(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CAAssignmentRequest request) {
        try {
            User admin = authUser.getUser();
            CAAssignmentResponse response = caAssignmentService.assignCAToCaUser(request, admin);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error assigning CA certificate: " + e.getMessage());
        }
    }

    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> revokeCAAssignment(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal AuthUser authUser) {
        try {
            User admin = authUser.getUser();
            caAssignmentService.revokeCAAssignment(assignmentId, admin);
            return ResponseEntity.ok().body("CA assignment revoked successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error revoking CA assignment: " + e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CAAssignmentResponse>> getAllAssignments() {
        List<CAAssignmentResponse> assignments = caAssignmentService.getAllAssignments();
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/organization/{organization}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CAAssignmentResponse>> getAssignmentsByOrganization(
            @PathVariable String organization) {
        List<CAAssignmentResponse> assignments = caAssignmentService.getAssignmentsByOrganization(organization);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CA')")
    public ResponseEntity<List<CAAssignmentResponse>> getMyAssignments(
            @AuthenticationPrincipal AuthUser authUser) {
        User caUser = authUser.getUser();
        List<CAAssignmentResponse> assignments = caAssignmentService.getAssignmentsForUser(caUser);
        return ResponseEntity.ok(assignments);
    }
}