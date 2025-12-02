package org.example.service.impl.strategy;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
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
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.example.enums.AuthProvider;
import org.example.exception.GmailServiceException;
import org.example.dto.response.EmailPageResponse;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class GoogleEmailStrategy implements EmailProviderStrategy {

  private final UserRepository userRepository;

  @Value("${google.client.id}")
  private String googleClientId;

  @Value("${google.client.secret}")
  private String googleClientSecret;

  @Value("${google.application.name}")
  private String applicationName;

  /**
   * Gets a valid Gmail client. If the access token is invalid/expired, it uses
   * the Refresh Token to
   * get a new one and updates the Database.
   */
  private Gmail getGmailClient(User user) {
    try {
      GoogleCredential credential = new GoogleCredential().setAccessToken(user.getGoogleAccessToken());

      // 1. Build the Gmail service with current token
      Gmail service = new Gmail.Builder(
          GoogleNetHttpTransport.newTrustedTransport(),
          GsonFactory.getDefaultInstance(),
          credential)
          .setApplicationName(applicationName)
          .build();

      // 2. Proactive Check: Try a lightweight call (e.g., get Profile) to check
      // validity
      // Or, simply catch the 401 error in the actual method.
      // Here, we will assume the token is valid, but if we catch an exception below,
      // we refresh.
      return service;

    } catch (Exception e) {
      throw new RuntimeException("Error building Gmail client", e);
    }
  }

  /** Helper to refresh token and save to DB */
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

    // Save to DB immediately so subsequent calls use the new one
    user.setGoogleAccessToken(newAccessToken);
    userRepository.save(user);

    return newAccessToken;
  }

  @Override
  public List<Label> getLabels(User user) {
    try {
      return executeGetLabels(user);
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        try {
          refreshAccessToken(user);
          return executeGetLabels(user); // Retry with new token from updated user object
        } catch (IOException | GeneralSecurityException ioException) {
          throw new RuntimeException("Failed to refresh token", ioException);
        }
      } else {
        throw new GmailServiceException(e);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected Error", e);

    }
  }

  private List<Label> executeGetLabels(User user)
      throws IOException, java.security.GeneralSecurityException {
    Gmail service = getGmailClient(user);
    return service.users().labels().list("me").execute().getLabels();
  }

  @Override
  public EmailPageResponse getEmails(User user, String labelId, String pageToken, int limit) {
    try {
      return executeGetEmails(user, labelId, pageToken, limit);
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        try {
          refreshAccessToken(user);
          return executeGetEmails(user, labelId, pageToken, limit);
        } catch (IOException ioException) {
          throw new RuntimeException("Failed to refresh token", ioException);
        } catch (GeneralSecurityException ex) {
          throw new RuntimeException(ex);
        }
      } else {
        throw new GmailServiceException(e);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected Error", e);
    }
  }

  private EmailPageResponse executeGetEmails(User user, String labelId, String pageToken, int limit)
      throws IOException, java.security.GeneralSecurityException {
    Gmail service = getGmailClient(user);

    // Convert user provided limit to Long
    long maxResults = (long) limit;

    var listRequest = service
        .users()
        .messages()
        .list("me")
        .setLabelIds(List.of(labelId))
        .setMaxResults(maxResults);

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

    // Use CompletableFuture to fetch details concurrently
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

  @Override
  public Message getEmailDetails(User user, String messageId) {
    try {
      return executeGetDetail(user, messageId);
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        try {
          refreshAccessToken(user);
          return executeGetDetail(user, messageId);
        } catch (IOException ioException) {
          throw new RuntimeException("Failed to refresh token", ioException);
        }
      } else {
        throw new GmailServiceException(e);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected Error", e);
    }
  }

  private Message executeGetDetail(User user, String messageId) throws IOException {
    Gmail service = getGmailClient(user);
    return service.users().messages().get("me", messageId).execute();
  }

  @Override
  public AuthProvider getSupportedProvider() {
    return AuthProvider.GOOGLE;
  }

  @Override
  public void markAsRead(User user, String messageId) {
    try {
      executeMarkAsRead(user, messageId);
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        try {
          refreshAccessToken(user);
          executeMarkAsRead(user, messageId);
        } catch (IOException ioException) {
          throw new RuntimeException("Failed to refresh token", ioException);
        }
      } else {
        throw new GmailServiceException(e);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected Error", e);
    }
  }

  private void executeMarkAsRead(User user, String messageId) throws IOException {
    Gmail service = getGmailClient(user);
    var modifyRequest = new com.google.api.services.gmail.model.ModifyMessageRequest();
    modifyRequest.setRemoveLabelIds(List.of("UNREAD"));
    service.users().messages().modify("me", messageId, modifyRequest).execute();
  }

  @Override
  public void markAsUnread(User user, String messageId) {
    try {
      executeMarkAsUnread(user, messageId);
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        try {
          refreshAccessToken(user);
          executeMarkAsUnread(user, messageId);
        } catch (IOException ioException) {
          throw new RuntimeException("Failed to refresh token", ioException);
        }
      } else {
        throw new GmailServiceException(e);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected Error", e);
    }
  }

  private void executeMarkAsUnread(User user, String messageId) throws IOException {
    Gmail service = getGmailClient(user);
    var modifyRequest = new com.google.api.services.gmail.model.ModifyMessageRequest();
    modifyRequest.setAddLabelIds(List.of("UNREAD"));
    service.users().messages().modify("me", messageId, modifyRequest).execute();
  }

  @Override
  public void toggleStar(User user, String messageId, boolean starred) {
    try {
      executeToggleStar(user, messageId, starred);
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        try {
          refreshAccessToken(user);
          executeToggleStar(user, messageId, starred);
        } catch (IOException ioException) {
          throw new RuntimeException("Failed to refresh token", ioException);
        }
      } else {
        throw new GmailServiceException(e);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected Error", e);
    }
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

  @Override
  public void deleteEmail(User user, String messageId) {
    try {
      executeDeleteEmail(user, messageId);
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        try {
          refreshAccessToken(user);
          executeDeleteEmail(user, messageId);
        } catch (IOException ioException) {
          throw new RuntimeException("Failed to refresh token", ioException);
        }
      } else {
        throw new GmailServiceException(e);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected Error", e);
    }
  }

  private void executeDeleteEmail(User user, String messageId) throws IOException {
    Gmail service = getGmailClient(user);
    service.users().messages().trash("me", messageId).execute();
  }

  @Override
  public void untrashEmail(User user, String messageId) {
    try {
      executeUntrashEmail(user, messageId);
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        try {
          refreshAccessToken(user);
          executeUntrashEmail(user, messageId);
        } catch (IOException ioException) {
          throw new RuntimeException("Failed to refresh token", ioException);
        }
      } else {
        throw new GmailServiceException(e);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected Error", e);
    }
  }

  private void executeUntrashEmail(User user, String messageId) throws IOException {
    Gmail service = getGmailClient(user);
    service.users().messages().untrash("me", messageId).execute();
  }

  @Override
  public void batchDeleteEmails(User user, List<String> messageIds) {
    try {
      Gmail service = getGmailClient(user);
      com.google.api.services.gmail.model.BatchModifyMessagesRequest batchRequest = new com.google.api.services.gmail.model.BatchModifyMessagesRequest()
          .setIds(messageIds)
          .setAddLabelIds(java.util.Collections.singletonList("TRASH"));
      service.users().messages().batchModify("me", batchRequest).execute();
    } catch (Exception e) {
      throw new RuntimeException("Failed to batch delete emails", e);
    }
  }

  @Override
  public void batchMarkAsRead(User user, List<String> messageIds) {
    try {
      Gmail service = getGmailClient(user);
      com.google.api.services.gmail.model.BatchModifyMessagesRequest batchRequest = new com.google.api.services.gmail.model.BatchModifyMessagesRequest()
          .setIds(messageIds)
          .setRemoveLabelIds(java.util.Collections.singletonList("UNREAD"));
      service.users().messages().batchModify("me", batchRequest).execute();
    } catch (Exception e) {
      throw new RuntimeException("Failed to batch mark emails as read", e);
    }
  }

  @Override
  public void batchMarkAsUnread(User user, List<String> messageIds) {
    try {
      Gmail service = getGmailClient(user);
      com.google.api.services.gmail.model.BatchModifyMessagesRequest batchRequest = new com.google.api.services.gmail.model.BatchModifyMessagesRequest()
          .setIds(messageIds)
          .setAddLabelIds(java.util.Collections.singletonList("UNREAD"));
      service.users().messages().batchModify("me", batchRequest).execute();
    } catch (Exception e) {
      throw new RuntimeException("Failed to batch mark emails as unread", e);
    }
  }

  @Override
  public byte[] getAttachment(User user, String messageId, String attachmentId) {
    try {
      return executeGetAttachment(user, messageId, attachmentId);
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        try {
          refreshAccessToken(user);
          return executeGetAttachment(user, messageId, attachmentId);
        } catch (IOException ioException) {
          throw new RuntimeException("Failed to refresh token", ioException);
        }
      } else {
        throw new GmailServiceException(e);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected Error", e);
    }
  }

  private byte[] executeGetAttachment(User user, String messageId, String attachmentId) throws IOException {
    Gmail service = getGmailClient(user);
    var attachmentPart = service.users().messages().attachments().get("me", messageId, attachmentId).execute();
    return attachmentPart.decodeData();
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
    try {
      executeSendEmail(user, to, cc, bcc, subject, body, attachments);
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        try {
          refreshAccessToken(user);
          executeSendEmail(user, to, cc, bcc, subject, body, attachments);
        } catch (IOException | MessagingException ioException) {
          throw new RuntimeException("Failed to refresh token or send email", ioException);
        }
      } else {
        throw new GmailServiceException(e);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected Error", e);
    }
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

  @Override
  public void replyEmail(
      User user,
      String messageId,
      List<String> to,
      List<String> cc,
      List<String> bcc,
      String body,
      List<MultipartFile> attachments) {
    try {
      executeReplyEmail(user, messageId, to, cc, bcc, body, attachments);
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        try {
          refreshAccessToken(user);
          executeReplyEmail(user, messageId, to, cc, bcc, body, attachments);
        } catch (IOException | MessagingException ioException) {
          throw new RuntimeException("Failed to refresh token or reply email", ioException);
        }
      } else {
        throw new GmailServiceException(e);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected Error", e);
    }
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

    // 1. Get the original message to find headers
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

    // Use provided 'to' list, or fallback to the original sender
    List<String> finalTo = (to != null && !to.isEmpty()) ? to : List.of(fallbackTo);

    MimeMessage email = createMimeMessage(finalTo, cc, bcc, subject, body, attachments);

    // Add headers for threading
    email.setHeader("In-Reply-To", originalMessageId);
    email.setHeader("References", references);

    sendMessage(service, email, originalMessage.getThreadId());
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

    // Add HTML Body
    MimeBodyPart bodyPart = new MimeBodyPart();
    bodyPart.setContent(body, "text/html; charset=utf-8");
    multipart.addBodyPart(bodyPart);

    // Add Attachments
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

  private void sendMessage(Gmail service, MimeMessage email) throws MessagingException, IOException {
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

  @Override
  public List<Message> getThreadMessages(User user, String threadId) {
    try {
      return executeGetThreadMessages(user, threadId);
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        try {
          refreshAccessToken(user);
          return executeGetThreadMessages(user, threadId);
        } catch (IOException ioException) {
          throw new RuntimeException("Failed to refresh token", ioException);
        }
      } else {
        throw new GmailServiceException(e);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unexpected Error", e);
    }
  }

  private List<Message> executeGetThreadMessages(User user, String threadId) throws IOException {
    Gmail service = getGmailClient(user);
    // Fetch the thread with 'full' format to get all messages
    var thread = service.users().threads().get("me", threadId).setFormat("full").execute();
    return thread.getMessages();
  }
}
