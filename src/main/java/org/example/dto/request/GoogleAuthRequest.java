package org.example.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request object for Google authentication")
public class GoogleAuthRequest {

  @Schema(
      description = "Google authorization code",
      example = "4/0AX4XfW...",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String authCode;
}
