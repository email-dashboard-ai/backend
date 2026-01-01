package org.example.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request object for refreshing access tokens")
public class RefreshTokenRequest {

  @Schema(
      description = "Refresh token",
      example = "dXNlci1yZWZyZXNoLXRva2Vu...",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String token;
}
