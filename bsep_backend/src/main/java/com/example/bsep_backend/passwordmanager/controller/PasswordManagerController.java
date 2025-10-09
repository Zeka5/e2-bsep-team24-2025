package com.example.bsep_backend.passwordmanager.controller;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.passwordmanager.dto.CreatePasswordEntryRequest;
import com.example.bsep_backend.passwordmanager.dto.PasswordEntryResponse;
import com.example.bsep_backend.passwordmanager.service.PasswordManagerService;
import com.example.bsep_backend.security.user.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/password-manager")
@RequiredArgsConstructor
public class PasswordManagerController {

    private final PasswordManagerService passwordManagerService;

    @PostMapping("/entries")
    public ResponseEntity<?> createPasswordEntry(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreatePasswordEntryRequest request) {
        try {
            User user = authUser.getUser();
            PasswordEntryResponse response = passwordManagerService.createPasswordEntry(request, user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating password entry: " + e.getMessage());
        }
    }

    @GetMapping("/entries")
    public ResponseEntity<List<PasswordEntryResponse>> getUserPasswordEntries(
            @AuthenticationPrincipal AuthUser authUser) {
        User user = authUser.getUser();
        List<PasswordEntryResponse> entries = passwordManagerService.getUserPasswordEntries(user);
        return ResponseEntity.ok(entries);
    }

    @DeleteMapping("/entries/{entryId}")
    public ResponseEntity<?> deletePasswordEntry(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long entryId) {
        try {
            User user = authUser.getUser();
            passwordManagerService.deletePasswordEntry(entryId, user);
            return ResponseEntity.ok("Password entry deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting password entry: " + e.getMessage());
        }
    }
}
