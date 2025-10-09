package com.example.bsep_backend.service.intr;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.domain.UserSession;

import java.util.List;

public interface UserSessionService {
    UserSession createSession(User user, String ipAddress, String userAgent);
    void updateLastActivity(String sessionId);
    List<UserSession> getActiveSessions(Long userId);
    void revokeSession(String sessionId);
    boolean isSessionActive(String sessionId);
}
