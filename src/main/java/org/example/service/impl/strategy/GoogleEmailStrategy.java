package org.example.service.impl.strategy;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyMessageRequest;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMessage.RecipientType;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.checkerframework.checker.units.qual.s;
import org.example.dto.response.EmailPageResponse;
import org.example.enums.AuthProvider;
import org.example.exception.GmailNetworkException;
import org.example.exception.GmailServiceException;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleEmailStrategy implements EmailProviderStrategy {

  private final UserRepository userRepository;
  private final Map<String, String> snoozedLabelCache = new ConcurrentHashMap<>();

  @Value("${google.client.id}")
  private String googleClientId;

  @Value("${google.client.secret}")
  private String googleClientSecret;

  @Value("${google.application.name}")
  private String applicationName;

  @Override
  public AuthProvider getSupportedProvider() {
    return AuthProvider.GOOGLE;
  }

  // ===============================================================================================
  // PUBLIC API METHODS
  // ===============================================================================================

  @Override
  public List<Label> getLabels(User user) {
    return executeWithRetry(user, () -> executeGetLabels(user));
  }

  @Override
  public EmailPageResponse getEmails(User user, String labelId, String pageToken, int limit) {
    return executeWithRetry(user, () -> executeGetEmails(user, labelId, pageToken, limit));
  }

  @Override
  public Message getEmailDetails(User user, String messageId) {
    return executeWithRetry(user, () -> executeGetDetail(user, messageId));
  }

  @Override
  public List<Message> getThreadMessages(User user, String threadId) {
    return executeWithRetry(user, () -> executeGetThreadMessages(user, threadId));
  }

  @Override
  public byte[] getAttachment(User user, String messageId, String attachmentId) {
    return executeWithRetry(user, () -> executeGetAttachment(user, messageId, attachmentId));
  }

  @Override
  public void sendEmail(
      User user,
      List<String> to,
      List<String> cc,
      List<String> bcc,
      String subject,
      String body,
      List<MultipartFile> attachments) {
    executeWithRetry(
        user,
        () -> {
          executeSendEmail(user, to, cc, bcc, subject, body, attachments);
          return null;
        });
  }

  @Override
  public void replyEmail(
      User user,
      String messageId,
      List<String> to,
      List<String> cc,
      List<String> bcc,
      String body,
      List<MultipartFile> attachments) {
    executeWithRetry(
        user,
        () -> {
          executeReplyEmail(user, messageId, to, cc, bcc, body, attachments);
          return null;
        });
  }

  @Override
  public void markAsRead(User user, String messageId) {
    executeWithRetry(
        user,
        () -> {
          executeMarkAsRead(user, messageId);
          return null;
        });
  }

  @Override
  public void markAsUnread(User user, String messageId) {
    executeWithRetry(
        user,
        () -> {
          executeMarkAsUnread(user, messageId);
          return null;
        });
  }

  @Override
  public void toggleStar(User user, String messageId, boolean starred) {
    executeWithRetry(
        user,
        () -> {
          executeToggleStar(user, messageId, starred);
          return null;
        });
  }

  @Override
  public void deleteEmail(User user, String messageId) {
    executeWithRetry(
        user,
        () -> {
          executeDeleteEmail(user, messageId);
          return null;
        });
  }

  @Override
  public void untrashEmail(User user, String messageId) {
    executeWithRetry(
        user,
        () -> {
          executeUntrashEmail(user, messageId);
          return null;
        });
  }

  @Override
  public void batchDeleteEmails(User user, List<String> messageIds) {
    executeWithRetry(
        user,
        () -> {
          Gmail service = getGmailClient(user);
          com.google.api.services.gmail.model.BatchModifyMessagesRequest batchRequest = new com.google.api.services.gmail.model.BatchModifyMessagesRequest()
              .setIds(messageIds)
              .setAddLabelIds(java.util.Collections.singletonList("TRASH"));
          service.users().messages().batchModify("me", batchRequest).execute();
          return null;
        });
  }

  @Override
  public void batchMarkAsRead(User user, List<String> messageIds) {
    executeWithRetry(
        user,
        () -> {
          Gmail service = getGmailClient(user);
          com.google.api.services.gmail.model.BatchModifyMessagesRequest batchRequest = new com.google.api.services.gmail.model.BatchModifyMessagesRequest()
              .setIds(messageIds)
              .setRemoveLabelIds(java.util.Collections.singletonList("UNREAD"));
          service.users().messages().batchModify("me", batchRequest).execute();
          return null;
        });
  }

  @Override
  public void batchMarkAsUnread(User user, List<String> messageIds) {
    executeWithRetry(
        user,
        () -> {
          Gmail service = getGmailClient(user);
          com.google.api.services.gmail.model.BatchModifyMessagesRequest batchRequest = new com.google.api.services.gmail.model.BatchModifyMessagesRequest()
              .setIds(messageIds)
              .setAddLabelIds(java.util.Collections.singletonList("UNREAD"));
          service.users().messages().batchModify("me", batchRequest).execute();
          return null;
        });
  }

  // ===============================================================================================
  // PRIVATE METHODS
  // ===============================================================================================

  private List<Label> executeGetLabels(User user)
      throws IOException, java.security.GeneralSecurityException {
    Gmail service = getGmailClient(user);
    return service.users().labels().list("me").execute().getLabels();
  }

  private EmailPageResponse executeGetEmails(User user, String labelId, String pageToken, int limit)
      throws IOException, java.security.GeneralSecurityException {
    Gmail service = getGmailClient(user);

    long maxResults = (long) limit;

    var listRequest = service.users().messages().list("me").setLabelIds(List.of(labelId)).setMaxResults(maxResults);

    if (pageToken != null && !pageToken.isEmpty()) {
      listRequest.setPageToken(pageToken);
    }

    var response = listRequest.execute();

    List<Message> messages = response.getMessages();
    if (messages == null || messages.isEmpty()) {
      return EmailPageResponse.builder()
          .messages(new ArrayList<>())
          .nextPageToken(null)
          .build();
    }

    List<java.util.concurrent.CompletableFuture<Message>> futures = messages.stream()
        .map(
            msg -> java.util.concurrent.CompletableFuture.supplyAsync(
                () -> {
                  try {
                    return service
                        .users()
                        .messages()
                        .get("me", msg.getId())
                        .setFormat("metadata")
                        .setMetadataHeaders(List.of("Subject", "From", "To", "Date"))
                        .execute();
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                }))
        .toList();

    List<Message> detailedMessages = futures.stream().map(java.util.concurrent.CompletableFuture::join).toList();

    return EmailPageResponse.builder()
        .messages(detailedMessages)
        .nextPageToken(response.getNextPageToken())
        .build();
  }

  private Message executeGetDetail(User user, String messageId) throws IOException {
    Gmail service = getGmailClient(user);
    return service.users().messages().get("me", messageId).execute();
  }

  private List<Message> executeGetThreadMessages(User user, String threadId) throws IOException {
    Gmail service = getGmailClient(user);
    var thread = service.users().threads().get("me", threadId).setFormat("full").execute();
    return thread.getMessages();
  }

  private byte[] executeGetAttachment(User user, String messageId, String attachmentId)
      throws IOException {
    Gmail service = getGmailClient(user);
    var attachmentPart = service.users().messages().attachments().get("me", messageId, attachmentId).execute();
    return attachmentPart.decodeData();
  }

  private void executeSendEmail(
      User user,
      List<String> to,
      List<String> cc,
      List<String> bcc,
      String subject,
      String body,
      List<MultipartFile> attachments)
      throws IOException, MessagingException {
    Gmail service = getGmailClient(user);
    MimeMessage email = createMimeMessage(to, cc, bcc, subject, body, attachments);
    sendMessage(service, email);
  }

  private void executeReplyEmail(
      User user,
      String messageId,
      List<String> to,
      List<String> cc,
      List<String> bcc,
      String body,
      List<MultipartFile> attachments)
      throws IOException, MessagingException {
    Gmail service = getGmailClient(user);

    Message originalMessage = service
        .users()
        .messages()
        .get("me", messageId)
        .setFormat("metadata")
        .setMetadataHeaders(List.of("Subject", "Message-ID", "References", "From", "Reply-To"))
        .execute();

    String subject = "";
    String originalMessageId = "";
    String references = "";
    String fallbackTo = "";

    for (var header : originalMessage.getPayload().getHeaders()) {
      if (header.getName().equalsIgnoreCase("Reply-To")) {
        fallbackTo = header.getValue();
      } else if (header.getName().equalsIgnoreCase("From") && fallbackTo.isEmpty()) {
        fallbackTo = header.getValue();
      }
      if (header.getName().equalsIgnoreCase("Subject")) {
        subject = header.getValue();
      } else if (header.getName().equalsIgnoreCase("Message-ID")) {
        originalMessageId = header.getValue();
      } else if (header.getName().equalsIgnoreCase("References")) {
        references = header.getValue();
      }
    }

    if (!subject.toLowerCase().startsWith("re:")) {
      subject = "Re: " + subject;
    }

    if (references.isEmpty()) {
      references = originalMessageId;
    } else {
      references += " " + originalMessageId;
    }

    List<String> finalTo = (to != null && !to.isEmpty()) ? to : List.of(fallbackTo);

    MimeMessage email = createMimeMessage(finalTo, cc, bcc, subject, body, attachments);

    email.setHeader("In-Reply-To", originalMessageId);
    email.setHeader("References", references);

    sendMessage(service, email, originalMessage.getThreadId());
  }

  private void executeMarkAsRead(User user, String messageId) throws IOException {
    Gmail service = getGmailClient(user);
    var modifyRequest = new com.google.api.services.gmail.model.ModifyMessageRequest();
    modifyRequest.setRemoveLabelIds(List.of("UNREAD"));
    service.users().messages().modify("me", messageId, modifyRequest).execute();
  }

  private void executeMarkAsUnread(User user, String messageId) throws IOException {
    Gmail service = getGmailClient(user);
    var modifyRequest = new com.google.api.services.gmail.model.ModifyMessageRequest();
    modifyRequest.setAddLabelIds(List.of("UNREAD"));
    service.users().messages().modify("me", messageId, modifyRequest).execute();
  }

  private void executeToggleStar(User user, String messageId, boolean starred) throws IOException {
    Gmail service = getGmailClient(user);
    var modifyRequest = new com.google.api.services.gmail.model.ModifyMessageRequest();
    if (starred) {
      modifyRequest.setAddLabelIds(List.of("STARRED"));
    } else {
      modifyRequest.setRemoveLabelIds(List.of("STARRED"));
    }
    service.users().messages().modify("me", messageId, modifyRequest).execute();
  }

  private void executeDeleteEmail(User user, String messageId) throws IOException {
    Gmail service = getGmailClient(user);
    service.users().messages().trash("me", messageId).execute();
  }

  private void executeUntrashEmail(User user, String messageId) throws IOException {
    Gmail service = getGmailClient(user);
    service.users().messages().untrash("me", messageId).execute();
  }

  // ===============================================================================================
  // HELPER METHODS
  // ===============================================================================================

  private Gmail getGmailClient(User user) {
    try {
      GoogleCredential credential = new GoogleCredential().setAccessToken(user.getGoogleAccessToken());

      return new Gmail.Builder(
          GoogleNetHttpTransport.newTrustedTransport(),
          GsonFactory.getDefaultInstance(),
          credential)
          .setApplicationName(applicationName)
          .build();

    } catch (Exception e) {
      throw new RuntimeException("Error building Gmail client", e);
    }
  }

  private String refreshAccessToken(User user) throws IOException {
    if (user.getGoogleRefreshToken() == null) {
      throw new RuntimeException("No Google Refresh Token available for user " + user.getEmail());
    }

    GoogleTokenResponse response = new GoogleRefreshTokenRequest(
        new NetHttpTransport(),
        new GsonFactory(),
        user.getGoogleRefreshToken(),
        googleClientId,
        googleClientSecret)
        .execute();

    String newAccessToken = response.getAccessToken();

    user.setGoogleAccessToken(newAccessToken);
    userRepository.save(user);

    return newAccessToken;
  }

  private MimeMessage createMimeMessage(
      List<String> to,
      List<String> cc,
      List<String> bcc,
      String subject,
      String body,
      List<MultipartFile> attachments)
      throws MessagingException, IOException {
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);
    MimeMessage email = new MimeMessage(session);
    email.setFrom(new InternetAddress("me"));

    if (to != null) {
      for (String toEmail : to) {
        email.addRecipient(RecipientType.TO, new InternetAddress(toEmail));
      }
    }

    if (cc != null) {
      for (String ccEmail : cc) {
        email.addRecipient(RecipientType.CC, new InternetAddress(ccEmail));
      }
    }

    if (bcc != null) {
      for (String bccEmail : bcc) {
        email.addRecipient(RecipientType.BCC, new InternetAddress(bccEmail));
      }
    }

    email.setSubject(subject);

    MimeMultipart multipart = new MimeMultipart();

    MimeBodyPart bodyPart = new MimeBodyPart();
    bodyPart.setContent(body, "text/html; charset=utf-8");
    multipart.addBodyPart(bodyPart);

    if (attachments != null && !attachments.isEmpty()) {
      for (MultipartFile file : attachments) {
        MimeBodyPart attachmentPart = new MimeBodyPart();
        DataSource source = new ByteArrayDataSource(file.getBytes(), file.getContentType());
        attachmentPart.setDataHandler(new DataHandler(source));
        attachmentPart.setFileName(file.getOriginalFilename());
        multipart.addBodyPart(attachmentPart);
      }
    }

    email.setContent(multipart);
    return email;
  }

  private void sendMessage(Gmail service, MimeMessage email)
      throws MessagingException, IOException {
    sendMessage(service, email, null);
  }

  private void sendMessage(Gmail service, MimeMessage email, String threadId)
      throws MessagingException, IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    email.writeTo(buffer);
    byte[] bytes = buffer.toByteArray();
    String encodedEmail = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    Message message = new Message();
    message.setRaw(encodedEmail);
    if (threadId != null) {
      message.setThreadId(threadId);
    }

    service.users().messages().send("me", message).execute();
  }

  // ===============================================================================================
  // RETRY MECHANISM
  // ===============================================================================================

  @FunctionalInterface
  private interface GmailOperation<T> {
    T execute() throws Exception;
  }

  private <T> T executeWithRetry(User user, GmailOperation<T> operation) {
    try {
      return operation.execute();
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        try {
          refreshAccessToken(user);
          return operation.execute();
        } catch (Exception ex) {
          throw new RuntimeException("Failed to refresh token or retry operation", ex);
        }
      } else {
        throw new GmailServiceException(e);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected Error", e);
    }
  }

  private String createLabel(Gmail gmail, String labelName) {
    try {
      Label label = new Label()
          .setName(labelName)
          .setLabelListVisibility("labelShow")
          .setMessageListVisibility("show")
          .setType("user");

      Label createdLabel = gmail.users().labels().create("me", label).execute();

      log.info("Label created: {}", createdLabel.getId());
      return createdLabel.getId();

    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 409) {
        log.warn("Label '{}' already exists, fetching existing label", labelName);
        return findExistingLabelId(gmail, labelName);
      }
      throw new GmailServiceException(e);
    } catch (IOException e) {
      throw new GmailNetworkException(e);
    }
  }

  private String findExistingLabelId(Gmail gmail, String labelName) {
    try {
      List<Label> labels = gmail.users().labels()
          .list("me")
          .execute()
          .getLabels();

      for (Label label : labels) {
        if (label.getName().equals(labelName)) {
          return label.getId();
        }
      }

      log.error("Label '{}' should exist but not found", labelName);

      throw new RuntimeException("Label " + labelName + " should exist but not found");
    } catch (GoogleJsonResponseException e) {
      throw new GmailServiceException(e);
    } catch (IOException e) {
      throw new GmailNetworkException(e);
    }
  }

  @Override
  public String getSnoozedLabelId(User user) {
    String cacheKey = user.getEmail();

    if (snoozedLabelCache.containsKey(cacheKey)) {
      log.info("Using cached SNOOZED label Id for user: {}", cacheKey);
      return snoozedLabelCache.get(cacheKey);
    }

    try {
      Gmail gmail = getGmailClient(user);

      List<Label> labels = gmail.users().labels()
          .list("me")
          .execute()
          .getLabels();

      for (Label label : labels) {
        if ("SNOOZED".equals(label.getName())) {
          String labelId = label.getId();
          snoozedLabelCache.put(cacheKey, labelId);

          log.info("Found and cached SNOOZED label: {}", labelId);
          return labelId;
        }
      }

      log.info("SNOOZED label not found, create new one");

      String snoozedLabelId = createLabel(gmail, "SNOOZED");
      snoozedLabelCache.put(cacheKey, snoozedLabelId);

      return snoozedLabelId;
    } catch (GoogleJsonResponseException e) {
      throw new GmailNetworkException(e);
    } catch (IOException e) {
      throw new GmailNetworkException(e);
    }
  }

  @Override
  public void modifyLabels(User user, String emailId, List<String> addLabelIds, List<String> removeLabelIds) {
    if (emailId == null || emailId.trim().isEmpty()) {
      throw new IllegalArgumentException("Email ID is required.");
    }

    if ((addLabelIds == null || addLabelIds.isEmpty()) &&
        (removeLabelIds == null || removeLabelIds.isEmpty())) {
      log.warn("No labels to modify for email {}, skipping API call", emailId);
      return;
    }

    try {
      Gmail gmail = getGmailClient(user);

      ModifyMessageRequest request = new ModifyMessageRequest()
          .setAddLabelIds(addLabelIds)
          .setRemoveLabelIds(removeLabelIds);

      gmail.users().messages()
          .modify(emailId, emailId, request)
          .execute();

      log.info("Modified labels for email {}: added={}, removed={}",
          emailId, addLabelIds, removeLabelIds);
    } catch (GoogleJsonResponseException e) {
      throw new GmailServiceException(e);
    } catch (IOException e) {
      throw new GmailNetworkException(e);
    }
  }
}
