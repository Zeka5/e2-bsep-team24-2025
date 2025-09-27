package com.example.bsep_backend.pki.service;

import com.example.bsep_backend.domain.User;
import com.example.bsep_backend.pki.domain.KeystorePassword;
import com.example.bsep_backend.pki.repository.KeystorePasswordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeyStoreService {

    @Value("${pki.keystore.path:keystores}")
    private String keystorePath;

    private final KeystorePasswordRepository keystorePasswordRepository;
    private final PasswordEncryptionService passwordEncryptionService;

    @Transactional
    public void saveCAKeyStore(String alias, PrivateKey privateKey, Certificate certificate, User user) throws Exception {
        Path keystoreDir = Paths.get(keystorePath);
        if (!Files.exists(keystoreDir)) {
            Files.createDirectories(keystoreDir);
        }

        // Generate secure random passwords
        String keystorePassword = passwordEncryptionService.generateRandomPassword();
        String keyPassword = passwordEncryptionService.generateRandomPassword();

        // Create keystore
        String keystoreFileName = alias + ".jks";
        Path keystoreFile = keystoreDir.resolve(keystoreFileName);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, keystorePassword.toCharArray());

        keyStore.setKeyEntry(alias, privateKey, keyPassword.toCharArray(), new Certificate[]{certificate});

        try (FileOutputStream fos = new FileOutputStream(keystoreFile.toFile())) {
            keyStore.store(fos, keystorePassword.toCharArray());
        }

        // Encrypt and store passwords in database
        String userSpecificKey = passwordEncryptionService.generateUserSpecificKey(user.getId());
        String salt = passwordEncryptionService.generateSalt();

        String encryptedKeystorePassword = passwordEncryptionService.encryptPassword(keystorePassword, userSpecificKey, salt);
        String encryptedKeyPassword = passwordEncryptionService.encryptPassword(keyPassword, userSpecificKey, salt);

        // Store keystore password
        KeystorePassword keystorePasswordEntity = KeystorePassword.builder()
                .keystoreAlias(alias)
                .encryptedPassword(encryptedKeystorePassword + ":" + encryptedKeyPassword) // Store both passwords separated by colon
                .salt(salt)
                .user(user)
                .build();

        keystorePasswordRepository.save(keystorePasswordEntity);

        log.info("CA certificate and private key stored in keystore: {} with encrypted passwords", keystoreFile);
    }

    public KeyStore loadKeyStore(String alias) throws Exception {
        Path keystoreFile = Paths.get(keystorePath, alias + ".jks");
        if (!Files.exists(keystoreFile)) {
            throw new RuntimeException("Keystore not found: " + keystoreFile);
        }

        // Retrieve and decrypt password
        String keystorePassword = getKeystorePassword(alias);

        KeyStore keyStore = KeyStore.getInstance("JKS");

        try (FileInputStream fis = new FileInputStream(keystoreFile.toFile())) {
            keyStore.load(fis, keystorePassword.toCharArray());
        }

        return keyStore;
    }

    public PrivateKey getPrivateKey(String alias) throws Exception {
        KeyStore keyStore = loadKeyStore(alias);
        String keyPassword = getKeyPassword(alias);
        return (PrivateKey) keyStore.getKey(alias, keyPassword.toCharArray());
    }

    private String getKeystorePassword(String alias) throws Exception {
        KeystorePassword keystorePasswordEntity = keystorePasswordRepository.findByKeystoreAlias(alias)
                .orElseThrow(() -> new RuntimeException("Keystore password not found for alias: " + alias));

        String userSpecificKey = passwordEncryptionService.generateUserSpecificKey(keystorePasswordEntity.getUser().getId());
        String[] passwords = keystorePasswordEntity.getEncryptedPassword().split(":");

        return passwordEncryptionService.decryptPassword(passwords[0], userSpecificKey, keystorePasswordEntity.getSalt());
    }

    private String getKeyPassword(String alias) throws Exception {
        KeystorePassword keystorePasswordEntity = keystorePasswordRepository.findByKeystoreAlias(alias)
                .orElseThrow(() -> new RuntimeException("Key password not found for alias: " + alias));

        String userSpecificKey = passwordEncryptionService.generateUserSpecificKey(keystorePasswordEntity.getUser().getId());
        String[] passwords = keystorePasswordEntity.getEncryptedPassword().split(":");

        if (passwords.length < 2) {
            throw new RuntimeException("Invalid password format for alias: " + alias);
        }

        return passwordEncryptionService.decryptPassword(passwords[1], userSpecificKey, keystorePasswordEntity.getSalt());
    }

    @Transactional
    public void deleteKeystore(String alias) {
        try {
            Path keystoreFile = Paths.get(keystorePath, alias + ".jks");
            if (Files.exists(keystoreFile)) {
                Files.delete(keystoreFile);
            }
            keystorePasswordRepository.deleteByKeystoreAlias(alias);
            log.info("Keystore and passwords deleted for alias: {}", alias);
        } catch (Exception e) {
            log.error("Error deleting keystore for alias: {}", alias, e);
            throw new RuntimeException("Failed to delete keystore: " + alias, e);
        }
    }
}