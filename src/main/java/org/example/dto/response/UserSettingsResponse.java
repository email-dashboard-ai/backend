package org.example.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object containing user settings")
public class UserSettingsResponse {

  @Schema(description = "User's email address", example = "user@example.com")
  private String email;

  @Schema(
      description =
          "Custom AI summary prompt for email summarization. Null if using default prompt.",
      example = "Summarize this email in Vietnamese, focusing on action items and deadlines.")
  private String customSummaryPrompt;

  @Schema(
      description = "The default system prompt used when customSummaryPrompt is not set",
      example =
          "You are an email assistant. Summarize the following email in 1-2 concise sentences...")
  private String defaultPrompt;

  @Schema(
      description = "Whether the user is using a custom prompt or the default",
      example = "true")
  private boolean usingCustomPrompt;
}
