package com.example.bsep_backend.pki.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.UUID;

@Service
@Slf4j
public class KeyStoreService {

    @Value("${pki.keystore.path:keystores}")
    private String keystorePath;

    private static final String DEFAULT_KEYSTORE_PASSWORD = "password";
    private static final String DEFAULT_KEY_PASSWORD = "password";

    public void saveCAKeyStore(String alias, PrivateKey privateKey, Certificate certificate) throws Exception {
        Path keystoreDir = Paths.get(keystorePath);
        if (!Files.exists(keystoreDir)) {
            Files.createDirectories(keystoreDir);
        }

        String keystoreFileName = alias + ".jks";
        Path keystoreFile = keystoreDir.resolve(keystoreFileName);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, DEFAULT_KEYSTORE_PASSWORD.toCharArray());

        keyStore.setKeyEntry(alias, privateKey, DEFAULT_KEY_PASSWORD.toCharArray(), new Certificate[]{certificate});

        try (FileOutputStream fos = new FileOutputStream(keystoreFile.toFile())) {
            keyStore.store(fos, DEFAULT_KEYSTORE_PASSWORD.toCharArray());
        }

        log.info("CA certificate and private key stored in keystore: {}", keystoreFile);
    }

    public KeyStore loadKeyStore(String alias) throws Exception {
        Path keystoreFile = Paths.get(keystorePath, alias + ".jks");
        if (!Files.exists(keystoreFile)) {
            throw new RuntimeException("Keystore not found: " + keystoreFile);
        }

        KeyStore keyStore = KeyStore.getInstance("JKS");

        try (FileInputStream fis = new FileInputStream(keystoreFile.toFile())) {
            keyStore.load(fis, DEFAULT_KEYSTORE_PASSWORD.toCharArray());
        }

        return keyStore;
    }

    public PrivateKey getPrivateKey(String alias) throws Exception {
        KeyStore keyStore = loadKeyStore(alias);
        return (PrivateKey) keyStore.getKey(alias, DEFAULT_KEY_PASSWORD.toCharArray());
    }
}