package com.example.bsep_backend.passwordmanager.domain;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.pki.domain.Certificate;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "shared_password_entries")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SharedPasswordEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_entry_id", nullable = false)
    private PasswordEntry originalEntry;

    @Column(nullable = false)
    private String website;

    @Column(nullable = false)
    private String username;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String encryptedPassword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by_user_id", nullable = false)
    private User sharedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_user_id", nullable = false)
    private User sharedWithUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_certificate_id", nullable = false)
    private Certificate sharedWithCertificate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
