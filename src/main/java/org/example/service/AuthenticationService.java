package org.example.service;

import org.example.dto.request.AuthRequest;
import org.example.dto.request.GoogleAuthRequest;
import org.example.dto.request.RefreshTokenRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.response.AuthResponse;

import java.io.IOException;

public interface AuthenticationService {
    AuthResponse register(RegisterRequest request);

    AuthResponse authenticate(AuthRequest request);

    AuthResponse authenticateGoogle(GoogleAuthRequest request) throws IOException;

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String userEmail);
}
