package com.example.bsep_backend.controller;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.domain.UserSession;
import com.example.bsep_backend.dto.UserSessionDto;
import com.example.bsep_backend.service.intr.UserService;
import com.example.bsep_backend.service.intr.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class UserSessionController {
    private final UserSessionService userSessionService;
    private final UserService userService;

    @GetMapping("/active")
    public ResponseEntity<List<UserSessionDto>> getActiveSessions() {
        User user = userService.getLogedInUser();
        List<UserSession> sessions = userSessionService.getActiveSessions(user.getId());

        List<UserSessionDto> sessionDtos = sessions.stream()
                .map(session -> UserSessionDto.builder()
                        .sessionId(session.getSessionId())
                        .ipAddress(session.getIpAddress())
                        .deviceType(session.getDeviceType())
                        .browser(session.getBrowser())
                        .createdAt(session.getCreatedAt())
                        .lastActivity(session.getLastActivity())
                        .build())
                .toList();

        return ResponseEntity.ok(sessionDtos);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<String> revokeSession(@PathVariable String sessionId) {
        userSessionService.revokeSession(sessionId);
        return ResponseEntity.ok("Session revoked successfully");
    }
}
