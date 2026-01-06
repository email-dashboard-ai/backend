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
@Schema(description = "Request object for user registration")
public class RegisterRequest {

  @Schema(
      description = "User's full name",
      example = "John Doe",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(
      description = "User's email address",
      example = "john.doe@example.com",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String email;

  @Schema(
      description = "User's password",
      example = "securePassword123",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String password;
}
