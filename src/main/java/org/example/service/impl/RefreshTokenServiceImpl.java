package org.example.service.impl;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.exception.TokenRefreshException;
import org.example.model.RefreshToken;
import org.example.model.User;
import org.example.repository.RefreshTokenRepository;
import org.example.repository.UserRepository;
import org.example.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

  @Value("${jwt.refresh-expiration}")
  private Long refreshTokenDurationMs;

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;

  /** Per-token locks to prevent concurrent refresh token processing */
  private final ConcurrentHashMap<String, ReentrantLock> tokenLocks = new ConcurrentHashMap<>();

  /** Short-lived cache to return same access token for concurrent requests (5 seconds TTL) */
  private final ConcurrentHashMap<String, CachedTokenResponse> tokenCache =
      new ConcurrentHashMap<>();

  private static class CachedTokenResponse {
    final String accessToken;
    final long expiryTime;

    CachedTokenResponse(String accessToken, long ttlMs) {
      this.accessToken = accessToken;
      this.expiryTime = System.currentTimeMillis() + ttlMs;
    }

    boolean isValid() {
      return System.currentTimeMillis() < expiryTime;
    }
  }

  public RefreshToken createRefreshToken(String userEmail) {
    User user =
        userRepository
            .findByEmail(userEmail)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    // Check if a refresh token already exists for the user
    RefreshToken existingToken = refreshTokenRepository.findByUser(user).orElse(null);
    if (existingToken != null) {
      existingToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
      log.info("Updated refresh token for user: {}", userEmail);
      return refreshTokenRepository.save(existingToken);
    }

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setUser(user);
    refreshToken.setToken(UUID.randomUUID().toString());
    refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
    log.info("Created new refresh token for user: {}", userEmail);
    return refreshTokenRepository.save(refreshToken);
  }

  public Optional<RefreshToken> findByToken(String token) {
    return refreshTokenRepository.findByToken(token);
  }

  public RefreshToken verifyExpiration(RefreshToken token) {
    if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
      refreshTokenRepository.delete(token);
      log.warn("Refresh token expired and deleted: {}", token.getToken());
      throw new TokenRefreshException(
          "Refresh token was expired. Please make a new signin request");
    }
    return token;
  }

  /**
   * Process refresh token with concurrency control. Ensures only one thread processes a given
   * refresh token at a time. Concurrent requests will wait and receive the same cached access
   * token.
   */
  @Override
  public String processRefreshTokenWithLock(
      String refreshTokenStr, RefreshTokenService.TokenGenerator tokenGenerator) {
    ReentrantLock lock = tokenLocks.computeIfAbsent(refreshTokenStr, k -> new ReentrantLock());
    lock.lock();
    try {
      // Check cache first (for concurrent requests within 5 seconds)
      CachedTokenResponse cached = tokenCache.get(refreshTokenStr);
      if (cached != null && cached.isValid()) {
        log.debug(
            "[CACHE HIT] Returning cached access token for refresh token: {}...",
            refreshTokenStr.substring(0, 8));
        return cached.accessToken;
      }

      // Verify and generate new access token
      RefreshToken refreshToken =
          findByToken(refreshTokenStr)
              .map(this::verifyExpiration)
              .orElseThrow(() -> new TokenRefreshException("Refresh token is not in database!"));

      String accessToken = tokenGenerator.generate(refreshToken.getUser());

      // Cache for 5 seconds to handle concurrent requests
      tokenCache.put(refreshTokenStr, new CachedTokenResponse(accessToken, 5000));
      log.debug(
          "[TOKEN GENERATED] New access token created for user: {}",
          refreshToken.getUser().getEmail());

      return accessToken;
    } finally {
      lock.unlock();
      // Clean up lock if no other thread is waiting
      tokenLocks.remove(refreshTokenStr, lock);
    }
  }

  /** Functional interface for token generation (to avoid circular dependency) */
  @FunctionalInterface
  public interface TokenGenerator {
    String generate(User user);
  }
}
