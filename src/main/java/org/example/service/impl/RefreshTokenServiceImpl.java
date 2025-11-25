package org.example.service.impl;

import lombok.RequiredArgsConstructor;

import org.example.model.RefreshToken;
import org.example.model.User;
import org.example.repository.RefreshTokenRepository;
import org.example.repository.UserRepository;
import org.example.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;


    public RefreshToken createRefreshToken(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Check if a refresh token already exists for the user
        RefreshToken existingToken = refreshTokenRepository.findByUser(user).orElse(null);
        if (existingToken != null) {
            // Update the existing token's expiry date and return it
            existingToken.setExpiryDate(Instant.now().plus(30, ChronoUnit.DAYS));
            return refreshTokenRepository.save(existingToken);
        }

        // Create a new refresh token if none exists
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plus(30, ChronoUnit.DAYS));
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }
}
