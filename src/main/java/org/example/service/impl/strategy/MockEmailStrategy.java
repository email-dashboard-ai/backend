package org.example.service.impl.strategy;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.enums.AuthProvider;
import org.example.helper.MockDataHelper;
import org.example.dto.response.EmailPageResponse;
import org.example.model.User;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MockEmailStrategy implements EmailProviderStrategy {

  private final MockDataHelper mockDataHelper;

  @Override
  public List<Label> getLabels(User user) {
    return mockDataHelper.getMockLabels();
  }

  @Override
  public EmailPageResponse getEmails(User user, String labelId, String pageToken, int limit) {
    int page = 1;
    try {
      if (pageToken != null && !pageToken.isEmpty()) {
        page = Integer.parseInt(pageToken);
      }
    } catch (NumberFormatException e) {
      // ignore
    }
    List<Message> messages = mockDataHelper.getMockMessages(labelId, page, limit);
    return EmailPageResponse.builder()
        .messages(messages)
        .nextPageToken(String.valueOf(page + 1)) // Simple mock pagination
        .build();
  }

  @Override
  public Message getEmailDetails(User user, String messageId) {
    return mockDataHelper.getMockMessageDetail(messageId);
  }

  @Override
  public AuthProvider getSupportedProvider() {
    return AuthProvider.LOCAL;
  }

  @Override
  public void markAsRead(User user, String messageId) {
    // Mock implementation - no-op
  }

  @Override
  public void markAsUnread(User user, String messageId) {
    // Mock implementation - no-op
  }

  @Override
  public void toggleStar(User user, String messageId, boolean starred) {
    // Mock implementation - no-op
  }

  @Override
  public void deleteEmail(User user, String messageId) {
    // Mock implementation - no-op
  }

  @Override
  public void untrashEmail(User user, String messageId) {
    // Mock implementation - no-op
  }

  @Override
  public void batchDeleteEmails(User user, List<String> messageIds) {
    // Mock implementation - no-op
  }

  @Override
  public void batchMarkAsRead(User user, List<String> messageIds) {
    // Mock implementation - no-op
  }

  @Override
  public void batchMarkAsUnread(User user, List<String> messageIds) {
    // Mock implementation - no-op
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
    // Mock implementation - no-op
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
    // Mock implementation - no-op
  }

  @Override
  public byte[] getAttachment(User user, String messageId, String attachmentId) {
    return new byte[0];
  }

  @Override
  public List<Message> getThreadMessages(User user, String threadId) {
    // Mock implementation: return a list containing a single mock message
    return List.of(getEmailDetails(user, threadId)); // Assuming threadId matches messageId for mock
  }
}
