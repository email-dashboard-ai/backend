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
@Schema(description = "Request object for updating user settings")
public class UpdateUserSettingsRequest {

  @Schema(
      description =
          "Custom AI summary prompt for email summarization. Set to null or empty to use default prompt.",
      example = "Summarize this email in Vietnamese, focusing on action items and deadlines.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String customSummaryPrompt;
}
