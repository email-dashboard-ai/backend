package org.example.service;

import java.util.Optional;
import org.example.model.RefreshToken;
import org.example.model.User;

public interface RefreshTokenService {

  RefreshToken createRefreshToken(String email);

  Optional<RefreshToken> findByToken(String token);

  RefreshToken verifyExpiration(RefreshToken token);

  /**
   * Process refresh token with concurrency control. Ensures only one thread processes a given
   * refresh token at a time. Concurrent requests will wait and receive the same cached access
   * token.
   */
  String processRefreshTokenWithLock(String refreshTokenStr, TokenGenerator tokenGenerator);

  /** Functional interface for token generation */
  @FunctionalInterface
  interface TokenGenerator {
    String generate(User user);
  }
}
