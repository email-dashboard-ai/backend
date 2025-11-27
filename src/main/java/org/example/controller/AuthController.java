package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.AuthRequest;
import org.example.dto.request.GoogleAuthRequest;
import org.example.dto.request.RefreshTokenRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.response.AuthResponse;
import org.example.dto.response.ResponseWrapper;
import org.example.service.AuthenticationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for Login, Register, Google Auth, and Token Management")
public class AuthController {

    private final AuthenticationService service;

    @Operation(summary = "Register a new user", description = "Creates a new local user account and returns JWT tokens.")
    @PostMapping("/register")
    public ResponseWrapper<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseWrapper.success(service.register(request), "User registered successfully");
    }

    @Operation(summary = "Login with Email/Password", description = "Authenticates a local user and returns JWT tokens.")
    @PostMapping("/login")
    public ResponseWrapper<AuthResponse> authenticate(@RequestBody AuthRequest request) {
        return ResponseWrapper.success(service.authenticate(request), "Login successful");
    }

    @Operation(summary = "Login with Google", description = "Exchanges a Google Auth Code for JWT tokens and setup offline access.")
    @PostMapping("/google")
    public ResponseWrapper<AuthResponse> googleAuth(@RequestBody GoogleAuthRequest request) throws Exception {
        return ResponseWrapper.success(service.authenticateGoogle(request), "Google login successful");
    }

    @Operation(summary = "Refresh Access Token", description = "Uses a valid Refresh Token to obtain a new Access Token.")
    @PostMapping("/refresh-token")
    public ResponseWrapper<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ResponseWrapper.success(service.refreshToken(request), "Token refreshed successfully");
    }

    @Operation(
            summary = "Logout",
            description = "Revokes the user's Refresh Token in the database. " +
                    "Frontend should listen for 200 OK and then trigger 'storage' event or BroadcastChannel to log out other tabs."
    )
    @PostMapping("/logout")
    public ResponseWrapper<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        // userDetails comes from the JWT Filter. If token is invalid, Filter blocks it before reaching here.
        if (userDetails != null) {
            log.info("Logging out user: {}", userDetails.getUsername());
            service.logout(userDetails.getUsername());
        }
        else {
            log.warn("Logout attempted without valid authentication.");
        }
        return ResponseWrapper.success(null, "Logout successful");
    }
}