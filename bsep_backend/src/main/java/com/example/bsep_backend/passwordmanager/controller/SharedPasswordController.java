package com.example.bsep_backend.passwordmanager.controller;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.passwordmanager.dto.SharePasswordRequest;
import com.example.bsep_backend.passwordmanager.dto.SharedPasswordEntryResponse;
import com.example.bsep_backend.passwordmanager.service.SharedPasswordService;
import com.example.bsep_backend.security.user.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shared-passwords")
@RequiredArgsConstructor
public class SharedPasswordController {

    private final SharedPasswordService sharedPasswordService;

    @PostMapping
    public ResponseEntity<?> sharePassword(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody SharePasswordRequest request) {
        try {
            User user = authUser.getUser();
            SharedPasswordEntryResponse response = sharedPasswordService.sharePassword(request, user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error sharing password: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<SharedPasswordEntryResponse>> getSharedPasswords(
            @AuthenticationPrincipal AuthUser authUser) {
        User user = authUser.getUser();
        List<SharedPasswordEntryResponse> sharedPasswords = sharedPasswordService.getSharedPasswordsForUser(user);
        return ResponseEntity.ok(sharedPasswords);
    }

    @DeleteMapping("/{sharedEntryId}")
    public ResponseEntity<?> deleteSharedPassword(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long sharedEntryId) {
        try {
            User user = authUser.getUser();
            sharedPasswordService.deleteSharedPassword(sharedEntryId, user);
            return ResponseEntity.ok("Shared password deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting shared password: " + e.getMessage());
        }
    }
}
