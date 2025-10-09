package com.example.bsep_backend.repository;

import com.example.bsep_backend.domain.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    List<UserSession> findByUserIdAndIsActiveTrue(Long userId);
    Optional<UserSession> findBySessionIdAndIsActiveTrue(String sessionId);
    void deleteBySessionId(String sessionId);
}
