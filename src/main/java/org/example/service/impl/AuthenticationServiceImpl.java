package org.example.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.transaction.Transactional;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.example.config.AppConfig;
import org.example.dto.request.AuthRequest;
import org.example.dto.request.GoogleAuthRequest;
import org.example.dto.request.RefreshTokenRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.response.AuthResponse;
import org.example.enums.AuthProvider;
import org.example.exception.TokenRefreshException;
import org.example.exception.UserAlreadyExistsException;
import org.example.model.RefreshToken;
import org.example.model.User;
import org.example.repository.RefreshTokenRepository;
import org.example.repository.UserRepository;
import org.example.security.JwtService;
import org.example.service.AuthenticationService;
import org.example.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {
  private final UserRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final RefreshTokenService refreshTokenService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final AppConfig appConfig;

  @Value("${google.client.id}")
  private String googleClientId;

  @Value("${google.client.secret}")
  private String googleClientSecret;

  @Override
  public AuthResponse register(RegisterRequest request) {
    log.info("Registering new user: {}", request.getEmail());
    if (repository.findByEmail(request.getEmail()).isPresent()) {
      throw new UserAlreadyExistsException("Email already in use: " + request.getEmail());
    }

    var user = new User();
    user.setName(request.getName());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setProvider(AuthProvider.LOCAL);
    repository.save(user);

    var jwtToken = jwtService.generateToken(user);
    var refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

    return AuthResponse.builder()
        .accessToken(jwtToken)
        .refreshToken(refreshToken.getToken())
        .build();
  }

  @Override
  public AuthResponse authenticate(AuthRequest request) {
    log.info("Authenticating user: {}", request.getEmail());
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

    var user = repository
        .findByEmail(request.getEmail())
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    var jwtToken = jwtService.generateToken(user);
    var refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

    return AuthResponse.builder()
        .accessToken(jwtToken)
        .refreshToken(refreshToken.getToken())
        .build();
  }

  @Override
  public AuthResponse authenticateGoogle(GoogleAuthRequest request) throws IOException {
    log.info("Authenticating with Google");
    GoogleAuthorizationCodeTokenRequest tokenRequest = new GoogleAuthorizationCodeTokenRequest(
        new NetHttpTransport(),
        new GsonFactory(),
        appConfig.getGoogle().getTokenUri(),
        googleClientId,
        googleClientSecret,
        request.getAuthCode(),
        appConfig.getGoogle().getRedirectUri());

    tokenRequest.setRequestInitializer(
        (HttpRequest httpRequest) -> {
          httpRequest.setConnectTimeout(appConfig.getGoogle().getConnectTimeoutMs());
          httpRequest.setReadTimeout(appConfig.getGoogle().getReadTimeoutMs());
        });

    GoogleTokenResponse tokenResponse = tokenRequest.execute();

    String email = tokenResponse.parseIdToken().getPayload().getEmail();
    String name = (String) tokenResponse.parseIdToken().getPayload().get("name");
    String picture = (String) tokenResponse.parseIdToken().getPayload().get("picture");

    User user = repository
        .findByEmail(email)
        .orElseGet(
            () -> {
              User newUser = new User();
              newUser.setEmail(email);
              newUser.setName(name);
              newUser.setAvatar(picture);
              newUser.setProvider(AuthProvider.GOOGLE);
              return newUser;
            });

    user.setGoogleAccessToken(tokenResponse.getAccessToken());
    if (tokenResponse.getRefreshToken() != null) {
      user.setGoogleRefreshToken(tokenResponse.getRefreshToken());
    }
    if (picture != null) {
      user.setAvatar(picture);
    }
    repository.save(user);

    var jwtToken = jwtService.generateToken(user);
    var refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

    return AuthResponse.builder()
        .accessToken(jwtToken)
        .refreshToken(refreshToken.getToken())
        .build();
  }

  @Override
  public AuthResponse refreshToken(RefreshTokenRequest request) {
    log.info("Refreshing token");
    return refreshTokenService
        .findByToken(request.getToken())
        .map(refreshTokenService::verifyExpiration)
        .map(RefreshToken::getUser)
        .map(
            user -> {
              String accessToken = jwtService.generateToken(user);
              return AuthResponse.builder()
                  .accessToken(accessToken)
                  .refreshToken(request.getToken())
                  .build();
            })
        .orElseThrow(() -> new TokenRefreshException("Refresh token is not in database!"));
  }

  @Override
  @Transactional
  public void logout(String userEmail) {
    var user = repository
        .findByEmail(userEmail)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    refreshTokenRepository.deleteByUser(user);
    log.info("User logged out: {}", userEmail);
  }
}
