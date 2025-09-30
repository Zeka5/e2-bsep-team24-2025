package com.example.bsep_backend.pki.domain;

import com.example.bsep_backend.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ca_assignments")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CAAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ca_user_id", nullable = false)
    private User caUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ca_certificate_id", nullable = false)
    private Certificate caCertificate;

    @Column(nullable = false)
    private String organization;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy;

    @Column(name = "is_active")
    private boolean isActive = true;
}