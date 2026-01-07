package org.example.service.impl;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.response.EmailPageResponse;
import org.example.enums.AuthProvider;
import org.example.model.SnoozedEmail;
import org.example.model.User;
import org.example.repository.SnoozedEmailRepository;
import org.example.repository.UserRepository;
import org.example.service.EmailService;
import org.example.service.impl.strategy.EmailProviderStrategy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {
  private final UserRepository userRepository;
  private final Map<AuthProvider, EmailProviderStrategy> strategies;
  private final SnoozedEmailRepository snoozedEmailRepository;
  private final org.example.repository.SyncedEmailRepository syncedEmailRepository;
  private final org.example.config.AppConfig appConfig;
  private final SearchOrchestrator searchOrchestrator;

  public EmailServiceImpl(
      UserRepository userRepository,
      List<EmailProviderStrategy> strategyList,
      SnoozedEmailRepository snoozedEmailRepository,
      org.example.repository.SyncedEmailRepository syncedEmailRepository,
      org.example.config.AppConfig appConfig,
      SearchOrchestrator searchOrchestrator) {
    this.userRepository = userRepository;
    this.snoozedEmailRepository = snoozedEmailRepository;
    this.syncedEmailRepository = syncedEmailRepository;
    this.appConfig = appConfig;
    this.searchOrchestrator = searchOrchestrator;

    // Convert list of strategies to a Map for O(1) lookup: { LOCAL -> MockStrategy,
    // GOOGLE ->
    // GoogleStrategy }
    this.strategies =
        strategyList.stream()
            .collect(
                Collectors.toMap(EmailProviderStrategy::getSupportedProvider, Function.identity()));
  }

  private EmailProviderStrategy getStrategy(User user) {
    return strategies.get(user.getProvider());
  }

  public List<Label> getLabels(String email) {
    User user = userRepository.findByEmail(email).orElseThrow();
    return getStrategy(user).getLabels(user);
  }

  public EmailPageResponse getEmails(String email, String labelId, String pageToken, int limit) {
    User user = userRepository.findByEmail(email).orElseThrow();
    EmailPageResponse response = getStrategy(user).getEmails(user, labelId, pageToken, limit);
    syncEmails(response.getMessages(), email);
    return response;
  }

  public Message getEmailDetails(String email, String messageId) {
    User user = userRepository.findByEmail(email).orElseThrow();
    Message message = getStrategy(user).getEmailDetails(user, messageId);
    syncEmail(message, email);
    return message;
  }

  public void markAsRead(String email, String messageId) {
    User user = userRepository.findByEmail(email).orElseThrow();
    getStrategy(user).markAsRead(user, messageId);
  }

  public void markAsUnread(String email, String messageId) {
    User user = userRepository.findByEmail(email).orElseThrow();
    getStrategy(user).markAsUnread(user, messageId);
  }

  public void toggleStar(String email, String messageId, boolean starred) {
    User user = userRepository.findByEmail(email).orElseThrow();
    getStrategy(user).toggleStar(user, messageId, starred);
  }

  public void deleteEmail(String email, String messageId) {
    User user = userRepository.findByEmail(email).orElseThrow();
    getStrategy(user).deleteEmail(user, messageId);
  }

  public void untrashEmail(String email, String messageId) {
    User user = userRepository.findByEmail(email).orElseThrow();
    getStrategy(user).untrashEmail(user, messageId);
  }

  @Override
  public void batchDeleteEmails(String email, List<String> messageIds) {
    User user = userRepository.findByEmail(email).orElseThrow();
    getStrategy(user).batchDeleteEmails(user, messageIds);
  }

  @Override
  public void batchMarkAsRead(String email, List<String> messageIds) {
    User user = userRepository.findByEmail(email).orElseThrow();
    getStrategy(user).batchMarkAsRead(user, messageIds);
  }

  @Override
  public void batchMarkAsUnread(String email, List<String> messageIds) {
    User user = userRepository.findByEmail(email).orElseThrow();
    getStrategy(user).batchMarkAsUnread(user, messageIds);
  }

  @Override
  public void sendEmail(
      String email,
      List<String> to,
      List<String> cc,
      List<String> bcc,
      String subject,
      String body,
      List<MultipartFile> attachments) {
    User user = userRepository.findByEmail(email).orElseThrow();
    getStrategy(user).sendEmail(user, to, cc, bcc, subject, body, attachments);
  }

  @Override
  public void replyEmail(
      String email,
      String messageId,
      List<String> to,
      List<String> cc,
      List<String> bcc,
      String body,
      List<MultipartFile> attachments) {
    User user = userRepository.findByEmail(email).orElseThrow();
    getStrategy(user).replyEmail(user, messageId, to, cc, bcc, body, attachments);
  }

  @Override
  public byte[] getAttachment(String email, String messageId, String attachmentId) {
    User user = userRepository.findByEmail(email).orElseThrow();
    return getStrategy(user).getAttachment(user, messageId, attachmentId);
  }

  @Override
  public List<Message> getThreadMessages(String email, String threadId) {
    User user = userRepository.findByEmail(email).orElseThrow();
    return getStrategy(user).getThreadMessages(user, threadId);
  }

  @Override
  public void snoozeEmail(String username, String emailId, Instant snoozedUntil) {

    User user =
        userRepository
            .findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    EmailProviderStrategy strategy = getStrategy(user);

    String snoozedLabelId = strategy.getSnoozedLabelId(user);

    strategy.modifyLabels(user, emailId, List.of(snoozedLabelId), List.of("INBOX"));

    SnoozedEmail snoozedEmail = new SnoozedEmail();
    snoozedEmail.setEmailId(emailId);
    snoozedEmail.setUser(user);
    snoozedEmail.setSnoozedUntil(snoozedUntil);

    snoozedEmailRepository.save(snoozedEmail);

    log.info("Snoozed email {} successfully unitl {}", emailId, snoozedUntil);
  }

  @Override
  public void unsnoozeEmail(String username, String emailId) {
    User user =
        userRepository
            .findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    SnoozedEmail snoozedEmail =
        snoozedEmailRepository
            .findByEmailIdAndUserEmail(emailId, username)
            .orElseThrow(() -> new IllegalStateException("Email is not snoozed"));

    EmailProviderStrategy strategy = getStrategy(user);
    String snoozedLabelId = strategy.getSnoozedLabelId(user);

    // Move email back to INBOX
    strategy.modifyLabels(user, emailId, List.of("INBOX"), List.of(snoozedLabelId));

    // Delete snooze record
    snoozedEmailRepository.delete(snoozedEmail);

    log.info("Unsnoozed email {} for user {}", emailId, username);
  }

  @Override
  public Map<String, Instant> getSnoozedEmailsInfo(String username) {
    List<SnoozedEmail> snoozedEmails = snoozedEmailRepository.findByUserEmail(username);
    return snoozedEmails.stream()
        .collect(Collectors.toMap(SnoozedEmail::getEmailId, SnoozedEmail::getSnoozedUntil));
  }

  @Override
  public List<org.example.dto.response.SearchResultDTO> search(
      String email, org.example.dto.request.SearchRequest request) {
    User user = userRepository.findByEmail(email).orElseThrow();
    SearchOrchestrator.SearchResult result = searchOrchestrator.resolve(request);

    log.info("Search: request={}, strategy={}", request, result.getStrategy());

    return switch (result.getStrategy()) {
      case GMAIL_API -> searchByGmailApi(user, result.getGmailQuery());
      case INTERNAL -> searchInternalWithSync(user, email, result.getFuzzyQuery());
      case HYBRID -> searchHybridWithSync(user, email, result.getGmailQuery(), result.getFuzzyQuery());
    };
  }

  private List<org.example.dto.response.SearchResultDTO> searchByGmailApi(
      User user, String gmailQuery) {
    var messages = getStrategy(user).searchByGmailQuery(user, gmailQuery);
    return messages.stream()
        .map(msg -> org.example.dto.response.SearchResultDTO.fromGmailMessage(msg, "GMAIL_API"))
        .collect(Collectors.toList());
  }

  /**
   * Search internal DB with pre-sync: fetches recent emails from Gmail and syncs them before
   * searching to ensure body content is available for fuzzy matching.
   */
  private List<org.example.dto.response.SearchResultDTO> searchInternalWithSync(
      User user, String email, String fuzzyQuery) {
    // Step 1: Fetch and sync recent emails from Gmail (synchronously)
    syncRecentEmailsForSearch(user, email);

    // Step 2: Search in synced DB
    var results = syncedEmailRepository.searchEmails(email, fuzzyQuery);
    return results.stream()
        .map(e -> org.example.dto.response.SearchResultDTO.fromSyncedEmail(e, "INTERNAL"))
        .collect(Collectors.toList());
  }

  private List<org.example.dto.response.SearchResultDTO> searchInternal(
      String email, String fuzzyQuery) {
    var results = syncedEmailRepository.searchEmails(email, fuzzyQuery);
    return results.stream()
        .map(e -> org.example.dto.response.SearchResultDTO.fromSyncedEmail(e, "INTERNAL"))
        .collect(Collectors.toList());
  }

  /**
   * Hybrid search with pre-sync: syncs Gmail search results before fuzzy matching on body.
   */
  private List<org.example.dto.response.SearchResultDTO> searchHybridWithSync(
      User user, String email, String gmailQuery, String fuzzyQuery) {
    // Step 1: Use Gmail API to get initial results (these have full body)
    var gmailMessages = getStrategy(user).searchByGmailQuery(user, gmailQuery);

    if (gmailMessages.isEmpty()) {
      return List.of();
    }

    // Step 2: Sync these messages SYNCHRONOUSLY so body is available for fuzzy search
    syncEmailsSync(gmailMessages, email);

    // Step 3: Extract message IDs from Gmail results
    var gmailMessageIds =
        gmailMessages.stream()
            .map(com.google.api.services.gmail.model.Message::getId)
            .collect(Collectors.toSet());

    // Step 4: Search internal DB for fuzzy match on body
    var internalResults = syncedEmailRepository.searchEmails(email, fuzzyQuery);

    // Step 5: Filter internal results to only include emails from Gmail results
    return internalResults.stream()
        .filter(e -> gmailMessageIds.contains(e.getMessageId()))
        .map(e -> org.example.dto.response.SearchResultDTO.fromSyncedEmail(e, "HYBRID"))
        .collect(Collectors.toList());
  }

  private List<org.example.dto.response.SearchResultDTO> searchHybrid(
      User user, String email, String gmailQuery, String fuzzyQuery) {
    // Step 1: Use Gmail API to get initial results
    var gmailMessages = getStrategy(user).searchByGmailQuery(user, gmailQuery);

    if (gmailMessages.isEmpty()) {
      return List.of();
    }

    // Extract message IDs from Gmail results
    var gmailMessageIds =
        gmailMessages.stream()
            .map(com.google.api.services.gmail.model.Message::getId)
            .collect(Collectors.toSet());

    // Step 2: Search internal DB for fuzzy match
    var internalResults = syncedEmailRepository.searchEmails(email, fuzzyQuery);

    // Step 3: Filter internal results to only include emails from Gmail results
    return internalResults.stream()
        .filter(e -> gmailMessageIds.contains(e.getMessageId()))
        .map(e -> org.example.dto.response.SearchResultDTO.fromSyncedEmail(e, "HYBRID"))
        .collect(Collectors.toList());
  }

  /**
   * Sync recent emails from Gmail SYNCHRONOUSLY before searching. This ensures that body content is
   * available in the database for fuzzy search.
   */
  private void syncRecentEmailsForSearch(User user, String userEmail) {
    try {
      // Fetch recent emails from INBOX (limit to avoid timeout)
      var response = getStrategy(user).getEmails(user, "INBOX", null, 50);
      if (response.getMessages() != null && !response.getMessages().isEmpty()) {
        syncEmailsSync(response.getMessages(), userEmail);
        log.debug("Pre-search sync: synced {} emails for user {}", 
            response.getMessages().size(), userEmail);
      }
    } catch (Exception e) {
      log.warn("Failed to pre-sync emails for search, continuing with existing data: {}", 
          e.getMessage());
      // Continue with search even if sync fails - use whatever is already in DB
    }
  }

  /**
   * Sync emails SYNCHRONOUSLY (blocking) - used for search to ensure data is available.
   */
  private void syncEmailsSync(List<Message> messages, String userEmail) {
    if (messages == null || messages.isEmpty()) return;
    try {
      List<org.example.model.SyncedEmail> syncedEmails =
          messages.stream()
              .map(msg -> toSyncedEmail(msg, userEmail))
              .filter(this::isWithinRetentionPeriod)
              .collect(Collectors.toList());
      if (!syncedEmails.isEmpty()) {
        // Use saveAll with try-catch for each to handle duplicates gracefully
        for (org.example.model.SyncedEmail email : syncedEmails) {
          try {
            syncedEmailRepository.save(email);
          } catch (Exception e) {
            // Ignore duplicate key errors, log others
            if (!e.getMessage().contains("duplicate") && !e.getMessage().contains("constraint")) {
              log.debug("Failed to sync email {}: {}", email.getMessageId(), e.getMessage());
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Failed to sync emails for user {}", userEmail, e);
    }
  }

  private void syncEmails(List<Message> messages, String userEmail) {
    if (messages == null || messages.isEmpty()) return;
    java.util.concurrent.CompletableFuture.runAsync(
        () -> {
          try {
            List<org.example.model.SyncedEmail> syncedEmails =
                messages.stream()
                    .map(msg -> toSyncedEmail(msg, userEmail))
                    .filter(this::isWithinRetentionPeriod)
                    .collect(Collectors.toList());
            if (!syncedEmails.isEmpty()) {
              syncedEmailRepository.saveAll(syncedEmails);
            }
          } catch (Exception e) {
            log.error("Failed to sync emails for user {}", userEmail, e);
          }
        });
  }

  private void syncEmail(Message message, String userEmail) {
    if (message == null) return;
    java.util.concurrent.CompletableFuture.runAsync(
        () -> {
          try {
            org.example.model.SyncedEmail syncedEmail = toSyncedEmail(message, userEmail);
            if (isWithinRetentionPeriod(syncedEmail)) {
              syncedEmailRepository.save(syncedEmail);
            }
          } catch (Exception e) {
            log.error("Failed to sync email {} for user {}", message.getId(), userEmail, e);
          }
        });
  }

  boolean isWithinRetentionPeriod(org.example.model.SyncedEmail email) {
    return email.getReceivedDate() != null
        && email
            .getReceivedDate()
            .isAfter(
                java.time.LocalDateTime.now().minusDays(appConfig.getSync().getRetentionDays()));
  }

  org.example.model.SyncedEmail toSyncedEmail(Message msg, String userEmail) {
    String subject = getHeader(msg, "Subject");
    String from = getHeader(msg, "From");
    String body = getBody(msg);
    // Fallback if body is empty, use snippet
    if (body == null || body.isBlank()) {
      body = msg.getSnippet();
    }

    return org.example.model.SyncedEmail.builder()
        .messageId(msg.getId())
        .userEmail(userEmail)
        .subject(subject)
        .from(from)
        .snippet(msg.getSnippet())
        .body(body)
        .receivedDate(
            java.time.LocalDateTime.ofInstant(
                Instant.ofEpochMilli(msg.getInternalDate()), java.time.ZoneId.systemDefault()))
        .build();
  }

  String getHeader(Message msg, String name) {
    if (msg.getPayload() == null || msg.getPayload().getHeaders() == null) return "";
    return msg.getPayload().getHeaders().stream()
        .filter(h -> h.getName().equalsIgnoreCase(name))
        .findFirst()
        .map(com.google.api.services.gmail.model.MessagePartHeader::getValue)
        .orElse("");
  }

  String getBody(Message msg) {
    if (msg.getPayload() == null) return "";
    String bodyPart = getBodyPart(msg.getPayload());
    if (bodyPart != null) {
      // Decode URL safe base64
      byte[] decoded = java.util.Base64.getUrlDecoder().decode(bodyPart);
      String html = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
      return org.jsoup.Jsoup.parse(html).text();
    }
    return msg.getSnippet();
  }

  private String getBodyPart(com.google.api.services.gmail.model.MessagePart part) {
    if (part.getMimeType().equalsIgnoreCase("text/plain")
        && part.getBody() != null
        && part.getBody().getData() != null) {
      return part.getBody().getData();
    }
    if (part.getMimeType().equalsIgnoreCase("text/html")
        && part.getBody() != null
        && part.getBody().getData() != null) {
      return part.getBody().getData();
    }
    if (part.getParts() != null) {
      for (com.google.api.services.gmail.model.MessagePart subPart : part.getParts()) {
        String result = getBodyPart(subPart);
        if (result != null) return result;
      }
    }
    return null;
  }
}
