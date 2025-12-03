package org.example.service.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.model.RefreshToken;
import org.example.model.User;
import org.example.repository.RefreshTokenRepository;
import org.example.repository.UserRepository;
import org.example.service.RefreshTokenService;
import org.example.exception.TokenRefreshException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

  @Value("${jwt.refresh-expiration}")
  private Long refreshTokenDurationMs;

  private final RefreshTokenRepository refreshTokenRepository;
  private final UserRepository userRepository;

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
      throw new TokenRefreshException("Refresh token was expired. Please make a new signin request");
    }
    return token;
  }
}
