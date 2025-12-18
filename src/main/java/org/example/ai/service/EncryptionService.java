package org.example.ai.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AES-256-GCM encryption service for protecting sensitive data at rest. Uses the JWT secret as the
 * encryption key source.
 */
@Slf4j
@Service
public class EncryptionService {
  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12; // 96 bits
  private static final int GCM_TAG_LENGTH = 128; // bits

  private final SecretKey secretKey;

  public EncryptionService(@Value("${jwt.secret}") String jwtSecret) {
    this.secretKey = deriveKey(jwtSecret);
  }

  /** Derive a 256-bit AES key from the JWT secret using SHA-256 */
  private SecretKey deriveKey(String secret) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
      return new SecretKeySpec(keyBytes, "AES");
    } catch (Exception e) {
      throw new RuntimeException("Failed to derive encryption key", e);
    }
  }

  /**
   * Encrypt plaintext using AES-256-GCM
   *
   * @param plaintext The text to encrypt
   * @return Base64-encoded ciphertext (IV + encrypted data)
   */
  public String encrypt(String plaintext) {
    if (plaintext == null || plaintext.isEmpty()) {
      return plaintext;
    }

    try {
      // Generate random IV
      byte[] iv = new byte[GCM_IV_LENGTH];
      new SecureRandom().nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      // Prepend IV to ciphertext
      byte[] combined = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception e) {
      log.error("Encryption failed", e);
      throw new RuntimeException("Encryption failed", e);
    }
  }

  /**
   * Decrypt ciphertext using AES-256-GCM
   *
   * @param ciphertext Base64-encoded ciphertext (IV + encrypted data)
   * @return Decrypted plaintext
   */
  public String decrypt(String ciphertext) {
    if (ciphertext == null || ciphertext.isEmpty()) {
      return ciphertext;
    }

    try {
      byte[] combined = Base64.getDecoder().decode(ciphertext);

      // Extract IV and ciphertext
      byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
      byte[] encrypted = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

      byte[] plaintext = cipher.doFinal(encrypted);
      return new String(plaintext, StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.error("Decryption failed", e);
      throw new RuntimeException("Decryption failed", e);
    }
  }
}
