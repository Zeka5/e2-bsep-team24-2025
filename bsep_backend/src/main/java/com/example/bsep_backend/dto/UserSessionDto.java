package com.example.bsep_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSessionDto {
    private String sessionId;
    private String ipAddress;
    private String deviceType;
    private String browser;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
}
