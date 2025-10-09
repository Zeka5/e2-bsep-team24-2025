package com.example.bsep_backend.passwordmanager.service;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.exception.NotFoundException;
import com.example.bsep_backend.passwordmanager.domain.PasswordEntry;
import com.example.bsep_backend.passwordmanager.domain.SharedPasswordEntry;
import com.example.bsep_backend.passwordmanager.dto.SharePasswordRequest;
import com.example.bsep_backend.passwordmanager.dto.SharedPasswordEntryResponse;
import com.example.bsep_backend.passwordmanager.repository.PasswordEntryRepository;
import com.example.bsep_backend.passwordmanager.repository.SharedPasswordEntryRepository;
import com.example.bsep_backend.pki.domain.Certificate;
import com.example.bsep_backend.pki.repository.CertificateRepository;
import com.example.bsep_backend.repository.UserRepository;
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
public class SharedPasswordService {

    private final SharedPasswordEntryRepository sharedPasswordEntryRepository;
    private final PasswordEntryRepository passwordEntryRepository;
    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final RSAPasswordEncryptionService rsaPasswordEncryptionService;

    @Transactional
    public SharedPasswordEntryResponse sharePassword(SharePasswordRequest request, User sharingUser) throws Exception {
        log.info("User {} is sharing password entry {} with user {}",
                sharingUser.getEmail(), request.getPasswordEntryId(), request.getSharedWithUserId());

        // Dobavi originalni password entry
        PasswordEntry originalEntry = passwordEntryRepository.findById(request.getPasswordEntryId())
                .orElseThrow(() -> new NotFoundException("Password entry not found"));

        // Proveri da li korisnik poseduje taj entry
        if (!originalEntry.getOwner().getId().equals(sharingUser.getId())) {
            throw new IllegalArgumentException("You can only share your own password entries");
        }

        // Dobavi korisnika sa kojim se deli
        User sharedWithUser = userRepository.findById(request.getSharedWithUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Dobavi sertifikat korisnika sa kojim se deli
        Certificate sharedWithCertificate = certificateRepository
                .findBySerialNumber(request.getSharedWithCertificateSerialNumber())
                .orElseThrow(() -> new NotFoundException("Certificate not found"));

        // Proveri da li sertifikat pripada korisniku sa kojim se deli
        if (!sharedWithCertificate.getOwner().getId().equals(sharedWithUser.getId())) {
            throw new IllegalArgumentException("Selected certificate does not belong to the target user");
        }

        log.info("Encrypting password with certificate: {} (SN: {})",
                sharedWithCertificate.getCommonName(), sharedWithCertificate.getSerialNumber());

        // Enkriptuj dekriptovanu lozinku sa javnim ključem primaoca
        String encryptedPassword = rsaPasswordEncryptionService.encryptPassword(
                request.getDecryptedPassword(),
                sharedWithCertificate.getCertificateData()
        );

        // Kreiraj shared entry
        SharedPasswordEntry sharedEntry = SharedPasswordEntry.builder()
                .originalEntry(originalEntry)
                .website(originalEntry.getWebsite())
                .username(originalEntry.getUsername())
                .encryptedPassword(encryptedPassword)
                .sharedByUser(sharingUser)
                .sharedWithUser(sharedWithUser)
                .sharedWithCertificate(sharedWithCertificate)
                .createdAt(LocalDateTime.now())
                .build();

        SharedPasswordEntry savedEntry = sharedPasswordEntryRepository.save(sharedEntry);
        log.info("Password shared successfully with user {}", sharedWithUser.getEmail());

        return mapToResponse(savedEntry);
    }

    @Transactional(readOnly = true)
    public List<SharedPasswordEntryResponse> getSharedPasswordsForUser(User user) {
        log.info("Getting shared passwords for user {}", user.getEmail());
        List<SharedPasswordEntry> sharedEntries = sharedPasswordEntryRepository
                .findBySharedWithUserIdWithDetails(user.getId());

        return sharedEntries.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSharedPassword(Long sharedEntryId, User user) {
        SharedPasswordEntry entry = sharedPasswordEntryRepository.findById(sharedEntryId)
                .orElseThrow(() -> new NotFoundException("Shared password entry not found"));

        // Samo vlasnik originalnog entry-ja ili primalac mogu da obrišu
        if (!entry.getSharedByUser().getId().equals(user.getId()) &&
            !entry.getSharedWithUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only delete your own shared password entries");
        }

        sharedPasswordEntryRepository.delete(entry);
        log.info("Shared password entry {} deleted by user {}", sharedEntryId, user.getEmail());
    }

    private SharedPasswordEntryResponse mapToResponse(SharedPasswordEntry entry) {
        return SharedPasswordEntryResponse.builder()
                .id(entry.getId())
                .website(entry.getWebsite())
                .username(entry.getUsername())
                .encryptedPassword(entry.getEncryptedPassword())
                .sharedByUserEmail(entry.getSharedByUser().getEmail())
                .sharedByUserName(entry.getSharedByUser().getName() + " " + entry.getSharedByUser().getSurname())
                .sharedWithCertificateSerialNumber(entry.getSharedWithCertificate().getSerialNumber())
                .sharedWithCertificateCommonName(entry.getSharedWithCertificate().getCommonName())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
