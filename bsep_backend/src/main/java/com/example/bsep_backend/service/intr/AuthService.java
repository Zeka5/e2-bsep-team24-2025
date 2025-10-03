package com.example.bsep_backend.service.intr;

import com.example.bsep_backend.dto.AuthData;
import com.example.bsep_backend.dto.LoginRequest;
import com.example.bsep_backend.dto.UserDto;

public interface AuthService {
    UserDto register(UserDto userDto);
    AuthData login(LoginRequest loginRequest, String ipAddress, String userAgent);
    String activateAccount(String activationToken);
    void logout(String sessionId);
}
