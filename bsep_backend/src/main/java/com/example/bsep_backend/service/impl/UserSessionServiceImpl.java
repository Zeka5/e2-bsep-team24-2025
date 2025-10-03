package com.example.bsep_backend.service.impl;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.domain.UserSession;
import com.example.bsep_backend.exception.NotFoundException;
import com.example.bsep_backend.repository.UserSessionRepository;
import com.example.bsep_backend.service.intr.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionServiceImpl implements UserSessionService {

    private final UserSessionRepository userSessionRepository;

    @Override
    @Transactional
    public UserSession createSession(User user, String ipAddress, String userAgent) {
        String sessionId = UUID.randomUUID().toString();

        UserSession session = UserSession.builder()
                .sessionId(sessionId)
                .user(user)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceType(extractDeviceType(userAgent))
                .browser(extractBrowser(userAgent))
                .createdAt(LocalDateTime.now())
                .lastActivity(LocalDateTime.now())
                .isActive(true)
                .build();

        return userSessionRepository.save(session);
    }

    @Override
    @Transactional
    public void updateLastActivity(String sessionId) {
        userSessionRepository.findBySessionIdAndIsActiveTrue(sessionId)
                .ifPresent(session -> {
                    session.setLastActivity(LocalDateTime.now());
                    userSessionRepository.save(session);
                });
    }

    @Override
    public List<UserSession> getActiveSessions(Long userId) {
        return userSessionRepository.findByUserIdAndIsActiveTrue(userId);
    }

    @Override
    @Transactional
    public void revokeSession(String sessionId) {
        UserSession session = userSessionRepository.findBySessionIdAndIsActiveTrue(sessionId)
                .orElse(null);

        // If session doesn't exist or is already revoked, just return (idempotent operation)
        if (session == null) {
            log.warn("Attempted to revoke non-existent or already revoked session: {}", sessionId);
            return;
        }

        session.setActive(false);
        userSessionRepository.save(session);
        log.info("Session revoked successfully: {}", sessionId);
    }

    @Override
    public boolean isSessionActive(String sessionId) {
        return userSessionRepository.findBySessionIdAndIsActiveTrue(sessionId).isPresent();
    }

    private String extractDeviceType(String userAgent) {
        if (userAgent == null) return "Unknown";

        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "Mobile";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "Tablet";
        }
        return "Desktop";
    }

    private String extractBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";

        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("edg")) {
            return "Edge";
        } else if (userAgent.contains("chrome")) {
            return "Chrome";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
        } else if (userAgent.contains("safari")) {
            return "Safari";
        } else if (userAgent.contains("opera") || userAgent.contains("opr")) {
            return "Opera";
        }
        return "Unknown";
    }
}
