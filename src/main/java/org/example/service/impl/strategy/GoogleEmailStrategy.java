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
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.enums.AuthProvider;
import org.example.exception.GmailServiceException;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
   * Gets a valid Gmail client. If the access token is invalid/expired, it uses the Refresh Token to
   * get a new one and updates the Database.
   */
  private Gmail getGmailClient(User user) {
    try {
      GoogleCredential credential =
          new GoogleCredential().setAccessToken(user.getGoogleAccessToken());

      // 1. Build the Gmail service with current token
      Gmail service =
          new Gmail.Builder(
                  GoogleNetHttpTransport.newTrustedTransport(),
                  GsonFactory.getDefaultInstance(),
                  credential)
              .setApplicationName(applicationName)
              .build();

      // 2. Proactive Check: Try a lightweight call (e.g., get Profile) to check validity
      // Or, simply catch the 401 error in the actual method.
      // Here, we will assume the token is valid, but if we catch an exception below, we refresh.
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

    GoogleTokenResponse response =
        new GoogleRefreshTokenRequest(
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
  public List<Message> getEmails(User user, String labelId, int page, int limit) {
    try {
      return executeGetEmails(user, labelId, limit);
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        try {
          refreshAccessToken(user);
          return executeGetEmails(user, labelId, limit);
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

  private List<Message> executeGetEmails(User user, String labelId, int limit)
      throws IOException, java.security.GeneralSecurityException {
    Gmail service = getGmailClient(user);

    // Convert user provided limit to Long
    long maxResults = (long) limit;

    var response =
        service
            .users()
            .messages()
            .list("me")
            .setLabelIds(List.of(labelId))
            .setMaxResults(maxResults)
            .execute();

    List<Message> messages = response.getMessages();
    if (messages == null || messages.isEmpty()) {
      return new ArrayList<>();
    }

    // Use CompletableFuture to fetch details concurrently
    List<java.util.concurrent.CompletableFuture<Message>> futures =
        messages.stream()
            .map(
                msg ->
                    java.util.concurrent.CompletableFuture.supplyAsync(
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

    return futures.stream()
        .map(java.util.concurrent.CompletableFuture::join)
        .collect(java.util.stream.Collectors.toList());
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
      com.google.api.services.gmail.model.BatchModifyMessagesRequest batchRequest =
          new com.google.api.services.gmail.model.BatchModifyMessagesRequest()
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
      com.google.api.services.gmail.model.BatchModifyMessagesRequest batchRequest =
          new com.google.api.services.gmail.model.BatchModifyMessagesRequest()
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
      com.google.api.services.gmail.model.BatchModifyMessagesRequest batchRequest =
          new com.google.api.services.gmail.model.BatchModifyMessagesRequest()
              .setIds(messageIds)
              .setAddLabelIds(java.util.Collections.singletonList("UNREAD"));
      service.users().messages().batchModify("me", batchRequest).execute();
    } catch (Exception e) {
      throw new RuntimeException("Failed to batch mark emails as unread", e);
    }
  }
}
