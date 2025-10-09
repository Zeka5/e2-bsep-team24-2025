package com.example.bsep_backend.passwordmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SharePasswordRequest {
    private Long passwordEntryId;
    private Long sharedWithUserId;
    private String sharedWithCertificateSerialNumber;
    private String decryptedPassword; // Dekriptovana lozinka sa fronta
}
