package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.ai.model.AiSummaryResult;
import org.example.ai.service.AiSummaryService;
import org.example.dto.request.AiEmailSummaryRequest;
import org.example.dto.response.AiEmailSummaryResponse;
import org.example.helper.ResponseWrapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Operations", description = "Endpoints for AI summaries")
@SecurityRequirement(name = "bearerAuth")
public class AiSummaryController {

  private final AiSummaryService aiSummaryService;

  @Operation(
      summary = "Summarize an email",
      description = "Returns a short AI summary for a single email.")
  @PostMapping("/email-summary")
  public ResponseWrapper<AiEmailSummaryResponse> summarizeEmail(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody AiEmailSummaryRequest request) {
    AiSummaryResult result =
        aiSummaryService.summarizeEmail(
            userDetails.getUsername(), request.getMessageId(), request.getContent());

    AiEmailSummaryResponse response =
        AiEmailSummaryResponse.builder()
            .messageId(request.getMessageId())
            .summary(result.getSummary())
            .provider(result.getProvider())
            .model(result.getModel())
            .cached(result.isCached())
            .source(result.getSource())
            .latencyMs(result.getLatencyMs())
            .build();

    return ResponseWrapper.success(response, "Email summarized successfully");
  }

  @Operation(
      summary = "Regenerate email summary",
      description =
          "Force regenerates AI summary bypassing cache. Uses user's custom prompt if set.")
  @PostMapping("/email-summary/regenerate")
  public ResponseWrapper<AiEmailSummaryResponse> regenerateSummary(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody AiEmailSummaryRequest request) {
    AiSummaryResult result =
        aiSummaryService.regenerateSummary(
            userDetails.getUsername(), request.getMessageId(), request.getContent());

    AiEmailSummaryResponse response =
        AiEmailSummaryResponse.builder()
            .messageId(request.getMessageId())
            .summary(result.getSummary())
            .provider(result.getProvider())
            .model(result.getModel())
            .cached(false)
            .source(result.getSource())
            .latencyMs(result.getLatencyMs())
            .build();

    return ResponseWrapper.success(response, "Email summary regenerated successfully");
  }
}
