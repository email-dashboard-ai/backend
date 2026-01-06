package org.example.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.example.dto.request.AuthRequest;
import org.example.dto.request.RegisterRequest;
import org.example.dto.response.AuthResponse;
import org.example.exception.UserAlreadyExistsException;
import org.example.model.RefreshToken;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.security.JwtService;
import org.example.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private RefreshTokenService refreshTokenService;

  @InjectMocks private AuthenticationServiceImpl authenticationService;

  @Test
  void register_ShouldThrowException_WhenEmailAlreadyExists() {
    // Given
    RegisterRequest request =
        RegisterRequest.builder()
            .name("Test User")
            .email("test@example.com")
            .password("password")
            .build();
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new User()));

    // When & Then
    assertThrows(
        UserAlreadyExistsException.class,
        () -> {
          authenticationService.register(request);
        });

    verify(userRepository, never()).save(any());
  }

  @Test
  void register_ShouldSaveUserAndReturnTokens_WhenEmailIsNew() {
    // Given
    RegisterRequest request =
        RegisterRequest.builder()
            .name("Test User")
            .email("test@example.com")
            .password("password")
            .build();
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
    when(jwtService.generateToken(any())).thenReturn("jwtToken");

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken("refreshToken");
    when(refreshTokenService.createRefreshToken("test@example.com")).thenReturn(refreshToken);

    // When
    AuthResponse response = authenticationService.register(request);

    // Then
    assertThat(response.getAccessToken()).isEqualTo("jwtToken");
    assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
    verify(userRepository).save(any(User.class));
  }

  @Test
  void authenticate_ShouldReturnTokens_WhenCredentialsAreValid() {
    // Given
    AuthRequest request =
        AuthRequest.builder().email("test@example.com").password("password").build();
    User user = new User();
    user.setEmail("test@example.com");

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(jwtService.generateToken(user)).thenReturn("jwtToken");

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken("refreshToken");
    when(refreshTokenService.createRefreshToken("test@example.com")).thenReturn(refreshToken);

    // When
    AuthResponse response = authenticationService.authenticate(request);

    // Then
    assertThat(response.getAccessToken()).isEqualTo("jwtToken");
    assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
    verify(authenticationManager).authenticate(any());
  }
}
