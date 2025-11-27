package org.example.service;

import java.util.Optional;
import org.example.model.RefreshToken;

public interface RefreshTokenService {

  RefreshToken createRefreshToken(String email);

  Optional<RefreshToken> findByToken(String token);

  RefreshToken verifyExpiration(RefreshToken token);
}
