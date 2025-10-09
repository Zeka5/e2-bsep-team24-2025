package com.example.bsep_backend.passwordmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePasswordEntryRequest {
    private String website;
    private String username;
    private String password;
    private String certificateSerialNumber;
}
