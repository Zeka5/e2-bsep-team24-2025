package com.example.bsep_backend.controller;

import com.example.bsep_backend.dto.AuthData;
import com.example.bsep_backend.dto.LoginRequest;
import com.example.bsep_backend.dto.UserDto;
import com.example.bsep_backend.security.jwt.JwtUtils;
import com.example.bsep_backend.service.intr.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody UserDto userDto) {
        return ResponseEntity.ok(authService.register(userDto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthData> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        return ResponseEntity.ok(authService.login(loginRequest, ipAddress, userAgent));
    }

    @GetMapping("/activate/{token}")
    public ResponseEntity<String> activateAccount(@PathVariable String token) {
        String message = authService.activateAccount(token);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token != null) {
            String sessionId = extractSessionIdFromToken(token);
            if (sessionId != null) {
                authService.logout(sessionId);
            }
        }
        return ResponseEntity.ok("Logged out successfully");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String extractSessionIdFromToken(String token) {
        return jwtUtils.getSessionIdFromToken(token);
    }
}
