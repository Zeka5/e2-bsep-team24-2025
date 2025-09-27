package com.example.bsep_backend.pki.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKeyFactory;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import jakarta.annotation.PostConstruct;

@Service
@Slf4j
public class PasswordEncryptionService {

    private String masterKey;

    @PostConstruct
    private void initializeMasterKey() {
        masterKey = System.getenv("PKI_MASTER_KEY");

        if (masterKey == null || masterKey.trim().isEmpty()) {
            // Fallback: generate a deterministic key based on system properties\
            String fallbackSeed = System.getProperty("user.name") +
                                 System.getProperty("os.name") +
                                 "PKI_FALLBACK_2024";
            masterKey = Base64.getEncoder().encodeToString(fallbackSeed.getBytes(StandardCharsets.UTF_8));
            log.warn("PKI_MASTER_KEY environment variable not set. Using fallback key. " +
                    "For production, set PKI_MASTER_KEY environment variable.");
        } else {
            log.info("Using PKI_MASTER_KEY from environment variable.");
        }
    }

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATION_COUNT = 100000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;

    public String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 16; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }

    public String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public String encryptPassword(String password, String userSpecificKey, String salt) throws Exception {
        SecretKey secretKey = deriveKeyFromUserKey(userSpecificKey, salt);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);

        // Generate IV
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] encryptedBytes = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));

        // Combine IV and encrypted data
        byte[] combined = new byte[IV_LENGTH + encryptedBytes.length];
        System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
        System.arraycopy(encryptedBytes, 0, combined, IV_LENGTH, encryptedBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public String decryptPassword(String encryptedPassword, String userSpecificKey, String salt) throws Exception {
        SecretKey secretKey = deriveKeyFromUserKey(userSpecificKey, salt);

        byte[] combined = Base64.getDecoder().decode(encryptedPassword);

        // Extract IV and encrypted data
        byte[] iv = new byte[IV_LENGTH];
        byte[] encryptedBytes = new byte[combined.length - IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
        System.arraycopy(combined, IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public String generateUserSpecificKey(Long userId) {
        // Generate a deterministic but unique key for each user based on their ID and master key
        String userKeyMaterial = masterKey + "_user_" + userId;
        return Base64.getEncoder().encodeToString(userKeyMaterial.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey deriveKeyFromUserKey(String userKey, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(
            userKey.toCharArray(),
            Base64.getDecoder().decode(salt),
            ITERATION_COUNT,
            KEY_LENGTH
        );
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }
}