package com.example.bsep_backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "users")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String username;
    @Column(unique = true)
    private String email;
    private String password;
    @Enumerated(EnumType.STRING)
    private UserRole role;
    @Column(name = "avatar_id")
    private Integer avatarId;
    @Column(name = "is_activated")
    private boolean isActivated = false;
    @Column(name = "activation_token")
    private String activationToken;
    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;
    @Column(name = "created_at")
    private final LocalDateTime createdAt = LocalDateTime.now();
}
