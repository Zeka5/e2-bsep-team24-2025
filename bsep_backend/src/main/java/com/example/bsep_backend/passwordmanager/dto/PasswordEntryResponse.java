package com.example.bsep_backend.passwordmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordEntryResponse {
    private Long id;
    private String website;
    private String username;
    private String encryptedPassword;
    private String certificateSerialNumber;
    private String certificateCommonName;
    private LocalDateTime createdAt;
}
