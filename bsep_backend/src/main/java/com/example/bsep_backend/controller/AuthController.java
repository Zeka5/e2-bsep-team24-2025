package com.example.bsep_backend.controller;

import com.example.bsep_backend.dto.AuthData;
import com.example.bsep_backend.dto.LoginRequest;
import com.example.bsep_backend.dto.UserDto;
import com.example.bsep_backend.service.intr.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody UserDto userDto) {
        return ResponseEntity.ok(authService.register(userDto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthData> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @GetMapping("/activate/{token}")
    public ResponseEntity<String> activateAccount(@PathVariable String token) {
        String message = authService.activateAccount(token);
        return ResponseEntity.ok(message);
    }
}
