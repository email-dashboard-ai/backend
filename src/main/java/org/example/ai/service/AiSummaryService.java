package org.example.ai.service;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ai.config.AiConfig;
import org.example.ai.exception.AiException;
import org.example.ai.model.AiSummaryResult;
import org.example.ai.provider.AiProviderRouter;
import org.example.ai.util.GmailMessageTextExtractor;
import org.example.ai.util.InMemoryTtlCache;
import org.example.enums.ErrorCode;
import org.example.model.EmailSummary;
import org.example.repository.EmailSummaryRepository;
import org.example.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSummaryService {
  private final AiConfig aiConfig;
  private final AiProviderRouter aiProviderRouter;
  private final EmailService emailService;
  private final EmailSummaryRepository emailSummaryRepository;
  private final EncryptionService encryptionService;

  private volatile InMemoryTtlCache<String, AiSummaryResult> cache;

  /** Per-key locks to prevent duplicate AI calls for the same email */
  private final ConcurrentHashMap<String, ReentrantLock> keyLocks = new ConcurrentHashMap<>();

  private InMemoryTtlCache<String, AiSummaryResult> cache() {
    if (cache == null) {
      cache =
          new InMemoryTtlCache<>(
              aiConfig.getCache().getMaxEntries(), aiConfig.getCache().getTtlSeconds());
    }
    return cache;
  }

  public AiSummaryResult summarizeEmail(String username, String messageId, String content) {
    if (messageId == null || messageId.isBlank()) {
      throw new AiException(
          HttpStatus.BAD_REQUEST, ErrorCode.ERR_AI_REQUEST_INVALID, "messageId is required");
    }

    String input = buildInput(username, messageId, content);
    String contentHash = sha256Hex(input);
    String cacheKey = messageId + ":" + contentHash;

    // L1: Check in-memory cache first (fastest)
    Optional<AiSummaryResult> memoryCached = cache().get(cacheKey);
    if (memoryCached.isPresent()) {
      log.debug("[CACHE HIT] messageId={}", messageId);
      AiSummaryResult existing = memoryCached.get();
      return AiSummaryResult.builder()
          .summary(existing.getSummary())
          .provider(existing.getProvider())
          .model(existing.getModel())
          .cached(true)
          .source("memory")
          .latencyMs(0)
          .build();
    }

    // L2: Check database (persists across restarts)
    Optional<EmailSummary> dbCached =
        emailSummaryRepository.findByMessageIdAndUserEmailAndContentHash(
            messageId, username, contentHash);
    if (dbCached.isPresent()) {
      log.debug("[DB HIT] messageId={}", messageId);
      EmailSummary existing = dbCached.get();
      // Decrypt summary from DB
      String decryptedSummary = encryptionService.decrypt(existing.getSummary());
      AiSummaryResult result =
          AiSummaryResult.builder()
              .summary(decryptedSummary)
              .provider(existing.getProvider())
              .model(existing.getModel())
              .cached(true)
              .source("database")
              .latencyMs(0)
              .build();
      // Populate L1 cache for faster subsequent access
      cache().put(cacheKey, result);
      return result;
    }

    // L3: Call AI provider with per-key lock to prevent duplicate calls
    // This ensures only ONE request calls AI for the same email, others wait and get cached result
    return callAiWithLock(cacheKey, messageId, username, contentHash, input);
  }

  /**
   * Calls AI provider with per-key locking to prevent duplicate AI calls for the same email. If
   * another thread is already processing the same key, this thread waits and then retrieves the
   * cached result.
   */
  private AiSummaryResult callAiWithLock(
      String cacheKey, String messageId, String username, String contentHash, String input) {
    ReentrantLock lock = keyLocks.computeIfAbsent(cacheKey, k -> new ReentrantLock());
    lock.lock();
    try {
      // Double-check cache after acquiring lock (another thread may have completed)
      Optional<AiSummaryResult> memoryCached = cache().get(cacheKey);
      if (memoryCached.isPresent()) {
        log.debug("[CACHE HIT after lock] messageId={}", messageId);
        AiSummaryResult existing = memoryCached.get();
        return AiSummaryResult.builder()
            .summary(existing.getSummary())
            .provider(existing.getProvider())
            .model(existing.getModel())
            .cached(true)
            .source("memory")
            .latencyMs(0)
            .build();
      }

      // Also check DB in case it was saved by another instance
      Optional<EmailSummary> dbCached =
          emailSummaryRepository.findByMessageIdAndUserEmailAndContentHash(
              messageId, username, contentHash);
      if (dbCached.isPresent()) {
        log.debug("[DB HIT after lock] messageId={}", messageId);
        EmailSummary existing = dbCached.get();
        String decryptedSummary = encryptionService.decrypt(existing.getSummary());
        AiSummaryResult result =
            AiSummaryResult.builder()
                .summary(decryptedSummary)
                .provider(existing.getProvider())
                .model(existing.getModel())
                .cached(true)
                .source("database")
                .latencyMs(0)
                .build();
        cache().put(cacheKey, result);
        return result;
      }

      // Now safe to call AI - we hold the lock
      log.debug("[AI CALL] messageId={}", messageId);
      AiSummaryResult result = aiProviderRouter.summarize(input);

      // Save to L1 cache
      cache().put(cacheKey, result);

      // Save to database for persistence (encrypted)
      // Double-check DB first to avoid sequence race condition
      try {
        // Check if already saved by another thread
        Optional<EmailSummary> existing =
            emailSummaryRepository.findByMessageIdAndUserEmailAndContentHash(
                messageId, username, contentHash);
        if (existing.isEmpty()) {
          // Encrypt summary before storing
          String encryptedSummary = encryptionService.encrypt(result.getSummary());
          emailSummaryRepository.insertIgnoreDuplicate(
              messageId,
              username,
              contentHash,
              encryptedSummary,
              result.getProvider(),
              result.getModel());
          log.debug("[DB SAVE] messageId={} (encrypted)", messageId);
        } else {
          log.debug("[DB SKIP] messageId={} already exists in DB", messageId);
        }
      } catch (Exception e) {
        // Don't fail the request if DB save fails - we still have the result
        log.warn("Failed to persist summary to DB for messageId={}: {}", messageId, e.getMessage());
      }

      return result;
    } finally {
      lock.unlock();
      // Clean up lock to prevent memory leak (only if no other thread is waiting)
      keyLocks.remove(cacheKey, lock);
    }
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
      throw new AiException(
          HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.ERR_SYSTEM, "Hashing failed", ex);
    }
  }
}
