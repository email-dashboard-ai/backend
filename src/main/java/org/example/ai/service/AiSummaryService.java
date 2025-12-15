package org.example.ai.service;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.ai.config.AiConfig;
import org.example.ai.exception.AiException;
import org.example.ai.model.AiSummaryResult;
import org.example.ai.provider.AiProviderRouter;
import org.example.ai.util.GmailMessageTextExtractor;
import org.example.ai.util.InMemoryTtlCache;
import org.example.enums.ErrorCode;
import org.example.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiSummaryService {
  private final AiConfig aiConfig;
  private final AiProviderRouter aiProviderRouter;
  private final EmailService emailService;

  private volatile InMemoryTtlCache<String, AiSummaryResult> cache;

  private InMemoryTtlCache<String, AiSummaryResult> cache() {
    if (cache == null) {
      cache = new InMemoryTtlCache<>(aiConfig.getCache().getMaxEntries(), aiConfig.getCache().getTtlSeconds());
    }
    return cache;
  }

  public AiSummaryResult summarizeEmail(String username, String messageId, String content) {
    if (messageId == null || messageId.isBlank()) {
      throw new AiException(HttpStatus.BAD_REQUEST, ErrorCode.ERR_AI_REQUEST_INVALID, "messageId is required");
    }

    String input = buildInput(username, messageId, content);
    String key = messageId + ":" + sha256Hex(input);

    Optional<AiSummaryResult> cached = cache().get(key);
    if (cached.isPresent()) {
      AiSummaryResult existing = cached.get();
      return AiSummaryResult.builder()
          .summary(existing.getSummary())
          .provider(existing.getProvider())
          .model(existing.getModel())
          .cached(true)
          .latencyMs(0)
          .build();
    }

    AiSummaryResult result = aiProviderRouter.summarize(input);
    cache().put(key, result);
    return result;
  }

  private String buildInput(String username, String messageId, String content) {
    String normalizedContent = normalize(content);
    if (!normalizedContent.isBlank()) {
      return truncate(normalizedContent, aiConfig.getMaxInputChars());
    }

    Message message = emailService.getEmailDetails(username, messageId);

    String subject = getHeader(message, "Subject");
    String from = getHeader(message, "From");

    String body =
        GmailMessageTextExtractor.extractBestEffortText(
            message,
            attachmentId -> emailService.getAttachment(username, messageId, attachmentId),
            aiConfig.getMaxInputChars());

    String combined =
        "Subject: "
            + (subject.isBlank() ? "(no subject)" : subject)
            + "\nFrom: "
            + (from.isBlank() ? "(unknown)" : from)
            + "\n\n"
            + body;

    return truncate(normalize(combined), aiConfig.getMaxInputChars());
  }

  private String getHeader(Message message, String name) {
    if (message == null || message.getPayload() == null) {
      return "";
    }
    List<MessagePartHeader> headers = message.getPayload().getHeaders();
    if (headers == null) {
      return "";
    }
    for (MessagePartHeader header : headers) {
      if (header != null
          && header.getName() != null
          && header.getName().equalsIgnoreCase(name)
          && header.getValue() != null) {
        return header.getValue();
      }
    }
    return "";
  }

  private String normalize(String s) {
    if (s == null) {
      return "";
    }
    return s.replaceAll("\\s+", " ").trim();
  }

  private String truncate(String s, int maxChars) {
    if (s == null) {
      return "";
    }
    if (maxChars <= 0 || s.length() <= maxChars) {
      return s;
    }
    return s.substring(0, maxChars);
  }

  private String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception ex) {
      throw new AiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.ERR_SYSTEM, "Hashing failed", ex);
    }
  }
}
