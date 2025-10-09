package com.example.bsep_backend.passwordmanager.service;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.exception.NotFoundException;
import com.example.bsep_backend.passwordmanager.domain.PasswordEntry;
import com.example.bsep_backend.passwordmanager.dto.CreatePasswordEntryRequest;
import com.example.bsep_backend.passwordmanager.dto.PasswordEntryResponse;
import com.example.bsep_backend.passwordmanager.repository.PasswordEntryRepository;
import com.example.bsep_backend.pki.domain.Certificate;
import com.example.bsep_backend.pki.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordManagerService {

    private final PasswordEntryRepository passwordEntryRepository;
    private final CertificateRepository certificateRepository;
    private final RSAPasswordEncryptionService rsaPasswordEncryptionService;

    @Transactional
    public PasswordEntryResponse createPasswordEntry(CreatePasswordEntryRequest request, User user) throws Exception {
        log.info("Creating password entry for user {} and website {}", user.getEmail(), request.getWebsite());

        // Dobavi sertifikat iz baze
        Certificate certificate = certificateRepository.findBySerialNumber(request.getCertificateSerialNumber())
                .orElseThrow(() -> new NotFoundException("Certificate not found"));
        log.info("Certificate found: {} (SN: {})", certificate.getCommonName(), certificate.getSerialNumber());

        // Proveri da li korisnik poseduje sertifikat
        if (!certificate.getOwner().getId().equals(user.getId())) {
            log.error("User {} tried to use certificate owned by user {}",
                    user.getId(), certificate.getOwner().getId());
            throw new IllegalArgumentException("You can only use your own certificates");
        }

        log.info("Starting password encryption...");
        // Enkriptuj lozinku javnim ključem iz sertifikata
        String encryptedPassword = rsaPasswordEncryptionService.encryptPassword(
                request.getPassword(),
                certificate.getCertificateData()
        );
        log.info("Password encrypted successfully");

        // Kreiraj password entry
        PasswordEntry passwordEntry = PasswordEntry.builder()
                .website(request.getWebsite())
                .username(request.getUsername())
                .encryptedPassword(encryptedPassword)
                .owner(user)
                .certificate(certificate)
                .createdAt(LocalDateTime.now())
                .build();

        PasswordEntry savedEntry = passwordEntryRepository.save(passwordEntry);
        log.info("Password entry created for user {} and website {}", user.getEmail(), request.getWebsite());

        return mapToResponse(savedEntry);
    }

    @Transactional(readOnly = true)
    public List<PasswordEntryResponse> getUserPasswordEntries(User user) {
        List<PasswordEntry> entries = passwordEntryRepository.findByOwnerIdWithCertificate(user.getId());
        return entries.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePasswordEntry(Long entryId, User user) {
        PasswordEntry entry = passwordEntryRepository.findById(entryId)
                .orElseThrow(() -> new NotFoundException("Password entry not found"));

        if (!entry.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only delete your own password entries");
        }

        passwordEntryRepository.delete(entry);
        log.info("Password entry {} deleted by user {}", entryId, user.getEmail());
    }

    private PasswordEntryResponse mapToResponse(PasswordEntry entry) {
        return PasswordEntryResponse.builder()
                .id(entry.getId())
                .website(entry.getWebsite())
                .username(entry.getUsername())
                .encryptedPassword(entry.getEncryptedPassword())
                .certificateSerialNumber(entry.getCertificate().getSerialNumber())
                .certificateCommonName(entry.getCertificate().getCommonName())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
