package org.example.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for user authentication")
public class AuthRequest {

  @Schema(
      description = "User's email address",
      example = "user@example.com",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String email;

  @Schema(
      description = "User's password",
      example = "password123",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String password;
}
