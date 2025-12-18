package org.example.ai.provider.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.ai.config.AiConfig;
import org.example.ai.exception.AiException;
import org.example.ai.model.AiProviderId;
import org.example.ai.model.AiSummaryResult;
import org.example.ai.provider.AiProvider;
import org.example.enums.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class GeminiAiProvider implements AiProvider {
  private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

  private final AiConfig aiConfig;
  private final ObjectMapper objectMapper;

  @Override
  public AiProviderId id() {
    return AiProviderId.GEMINI;
  }

  @Override
  public AiSummaryResult summarize(String inputText) {
    String apiKey = aiConfig.getGemini().getApiKey();
    if (apiKey == null || apiKey.isBlank()) {
      throw new AiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          ErrorCode.ERR_AI_CONFIG,
          "Missing GOOGLE_AI_STUDIO_API_KEY (ai.gemini.api-key)");
    }

    String model = aiConfig.getGemini().getModel();
    String prompt = aiConfig.getSummaryPrompt() + "\n\nEMAIL:\n" + inputText + "\n\nSUMMARY:";

    long start = System.nanoTime();
    try {
      String url =
          BASE_URL
              + "/models/"
              + model
              + ":generateContent?key="
              + java.net.URLEncoder.encode(apiKey, java.nio.charset.StandardCharsets.UTF_8);

      JsonNode payload =
          objectMapper
              .createObjectNode()
              .set(
                  "contents",
                  objectMapper
                      .createArrayNode()
                      .add(
                          objectMapper
                              .createObjectNode()
                              .put("role", "user")
                              .set(
                                  "parts",
                                  objectMapper
                                      .createArrayNode()
                                      .add(objectMapper.createObjectNode().put("text", prompt)))));

      ((com.fasterxml.jackson.databind.node.ObjectNode) payload)
          .set(
              "generationConfig",
              objectMapper
                  .createObjectNode()
                  .put("temperature", aiConfig.getGemini().getTemperature())
                  .put("maxOutputTokens", aiConfig.getGemini().getMaxOutputTokens()));

        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(aiConfig.getTimeoutMs()))
                .build()) {

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofMillis(aiConfig.getTimeoutMs()))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                            .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
        String body = truncate(response.body());
        int status = response.statusCode();
        if (status == 401 || status == 403) {
          throw new AiException(
              HttpStatus.SERVICE_UNAVAILABLE,
              ErrorCode.ERR_AI_CONFIG,
              "Gemini rejected the API key (HTTP "
                  + status
                  + "). Check GOOGLE_AI_STUDIO_API_KEY and that Generative Language API is enabled. Details: "
                  + body);
        }
        if (status == 429) {
          throw new AiException(
              HttpStatus.TOO_MANY_REQUESTS,
              ErrorCode.ERR_AI_SERVICE,
              "Gemini rate-limited the request (HTTP 429). Details: " + body);
        }
        if (status >= 400 && status < 500) {
          throw new AiException(
              HttpStatus.BAD_REQUEST,
              ErrorCode.ERR_AI_REQUEST_INVALID,
              "Gemini rejected the request (HTTP " + status + "). Details: " + body);
        }
        throw new AiException(
            HttpStatus.BAD_GATEWAY,
            ErrorCode.ERR_AI_SERVICE,
            "Gemini API error (HTTP " + status + "): " + body);
      }

      JsonNode root = objectMapper.readTree(response.body());
      String text =
          root.path("candidates")
              .path(0)
              .path("content")
              .path("parts")
              .path(0)
              .path("text")
              .asText("");

      long latencyMs = (System.nanoTime() - start) / 1_000_000L;
      return AiSummaryResult.builder()
          .summary(text.strip())
          .provider("gemini")
          .model(model)
          .cached(false)
          .source("api:gemini")
          .latencyMs(latencyMs)
          .build();
    } catch (AiException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new AiException(
          HttpStatus.BAD_GATEWAY,
          ErrorCode.ERR_AI_SERVICE,
          "Failed to call Gemini API",
          ex);
    }
  }

  private String truncate(String s) {
    if (s == null) {
      return "";
    }
    return s.length() > 500 ? s.substring(0, 500) + "…" : s;
  }
}
