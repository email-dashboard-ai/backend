package org.example.ai.provider.groq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.example.ai.config.AiConfig;
import org.example.ai.exception.AiException;
import org.example.ai.model.AiProviderId;
import org.example.ai.model.AiSummaryResult;
import org.example.ai.provider.AiProvider;
import org.example.enums.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroqAiProvider implements AiProvider {
  private static final String BASE_URL = "https://api.groq.com/openai/v1/chat/completions";

  private final AiConfig aiConfig;
  private final ObjectMapper objectMapper;

  @Override
  public AiProviderId id() {
    return AiProviderId.GROQ;
  }

  @Override
  public AiSummaryResult summarize(String inputText) {
    String apiKey = aiConfig.getGroq().getApiKey();
    if (apiKey == null || apiKey.isBlank()) {
      throw new AiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          ErrorCode.ERR_AI_CONFIG,
          "Missing GROQ_API_KEY (ai.groq.api-key)");
    }

    String model = aiConfig.getGroq().getModel();
    String prompt =
        "Summarize the following email for a Kanban card in 1-2 concise sentences. "
            + "Do not include greetings or signatures.\n\nEMAIL:\n"
            + inputText
            + "\n\nSUMMARY:";

    long start = System.nanoTime();
    try {
      // Build OpenAI-compatible payload for Groq
      JsonNode payload =
          objectMapper
              .createObjectNode()
              .put("model", model)
              .put("temperature", aiConfig.getGroq().getTemperature())
              .put("max_tokens", aiConfig.getGroq().getMaxOutputTokens())
              .set(
                  "messages",
                  objectMapper
                      .createArrayNode()
                      .add(
                          objectMapper
                              .createObjectNode()
                              .put("role", "user")
                              .put("content", prompt)));

      HttpClient client =
          HttpClient.newBuilder()
              .connectTimeout(Duration.ofMillis(aiConfig.getTimeoutMs()))
              .build();

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(BASE_URL))
              .timeout(Duration.ofMillis(aiConfig.getTimeoutMs()))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + apiKey)
              .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        String body = truncate(response.body());
        int status = response.statusCode();
        if (status == 401 || status == 403) {
          throw new AiException(
              HttpStatus.SERVICE_UNAVAILABLE,
              ErrorCode.ERR_AI_CONFIG,
              "Groq rejected the API key (HTTP " + status + "). Check GROQ_API_KEY. Details: " + body);
        }
        if (status == 429) {
          throw new AiException(
              HttpStatus.TOO_MANY_REQUESTS,
              ErrorCode.ERR_AI_SERVICE,
              "Groq rate-limited the request (HTTP 429). Details: " + body);
        }
        if (status >= 400 && status < 500) {
          throw new AiException(
              HttpStatus.BAD_REQUEST,
              ErrorCode.ERR_AI_REQUEST_INVALID,
              "Groq rejected the request (HTTP " + status + "). Details: " + body);
        }
        throw new AiException(
            HttpStatus.BAD_GATEWAY,
            ErrorCode.ERR_AI_SERVICE,
            "Groq API error (HTTP " + status + "): " + body);
      }

      // Parse OpenAI-compatible response
      JsonNode root = objectMapper.readTree(response.body());
      String text =
          root.path("choices")
              .path(0)
              .path("message")
              .path("content")
              .asText("");

      long latencyMs = (System.nanoTime() - start) / 1_000_000L;
      return AiSummaryResult.builder()
          .summary(text.strip())
          .provider("groq")
          .model(model)
          .cached(false)
          .latencyMs(latencyMs)
          .build();
    } catch (AiException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new AiException(
          HttpStatus.BAD_GATEWAY,
          ErrorCode.ERR_AI_SERVICE,
          "Failed to call Groq API",
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
