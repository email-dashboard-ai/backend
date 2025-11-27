package org.example.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request object for user authentication")
public class AuthRequest {

  @Schema(description = "User's email address", example = "user@example.com", required = true)
  private String email;

  @Schema(description = "User's password", example = "password123", required = true)
  private String password;
}
