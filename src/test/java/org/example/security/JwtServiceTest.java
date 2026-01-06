package org.example.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.Claims;
import org.example.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

  private JwtService jwtService;
  private final String TEST_SECRET =
      "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
  private final long TEST_EXPIRATION = 3600000; // 1 hour

  @BeforeEach
  void setUp() {
    jwtService = new JwtService();
    ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
    ReflectionTestUtils.setField(jwtService, "jwtExpiration", TEST_EXPIRATION);
  }

  @Test
  void generateToken_ShouldReturnValidToken() {
    // Given
    User user = new User();
    user.setEmail("test@example.com");

    // When
    String token = jwtService.generateToken(user);

    // Then
    assertThat(token).isNotEmpty();
    assertThat(jwtService.extractUsername(token)).isEqualTo("test@example.com");
  }

  @Test
  void isTokenValid_ShouldReturnTrue_WhenTokenMatchesUserAndNotExpired() {
    // Given
    User user = new User();
    user.setEmail("test@example.com");
    String token = jwtService.generateToken(user);

    // When
    boolean isValid = jwtService.isTokenValid(token, user);

    // Then
    assertThat(isValid).isTrue();
  }

  @Test
  void isTokenValid_ShouldReturnFalse_WhenUsernameDoesNotMatch() {
    // Given
    User user = new User();
    user.setEmail("test@example.com");
    String token = jwtService.generateToken(user);

    User otherUser = new User();
    otherUser.setEmail("other@example.com");

    // When
    boolean isValid = jwtService.isTokenValid(token, otherUser);

    // Then
    assertThat(isValid).isFalse();
  }

  @Test
  void extractAllClaims_ShouldThrowException_WhenTokenIsInvalid() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          jwtService.extractAllClaims("invalid-token");
        });
  }

  @Test
  void generateToken_WithAvatar_ShouldIncludeAvatarClaim() {
    // Given
    User user = new User();
    user.setEmail("test@example.com");
    user.setAvatar("https://example.com/avatar.png");

    // When
    String token = jwtService.generateToken(user);

    // Then
    Claims claims = jwtService.extractAllClaims(token);
    assertThat(claims.get("avatar")).isEqualTo("https://example.com/avatar.png");
  }
}
