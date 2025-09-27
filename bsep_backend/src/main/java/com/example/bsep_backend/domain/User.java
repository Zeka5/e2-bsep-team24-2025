package com.example.bsep_backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name must not exceed 50 characters")
    private String name;

    @NotBlank(message = "Surname is required")
    @Size(max = 50, message = "Surname must not exceed 50 characters")
    private String surname;

    @NotBlank(message = "Organization is required")
    @Size(max = 100, message = "Organization must not exceed 100 characters")
    private String organization;
    @Enumerated(EnumType.STRING)
    private UserRole role;
    @Column(name = "is_activated")
    private boolean isActivated = false;
    @Column(name = "activation_token")
    private String activationToken;
    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;
    @Column(name = "created_at")
    private final LocalDateTime createdAt = LocalDateTime.now();
}
