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
public class SharedPasswordEntryResponse {
    private Long id;
    private String website;
    private String username;
    private String encryptedPassword;
    private String sharedByUserEmail;
    private String sharedByUserName;
    private String sharedWithCertificateSerialNumber;
    private String sharedWithCertificateCommonName;
    private LocalDateTime createdAt;
}
