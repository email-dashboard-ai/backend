package org.example.service.impl.strategy;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.enums.AuthProvider;
import org.example.helper.MockDataHelper;
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
  public List<Message> getEmails(User user, String labelId, int page, int limit) {
    return mockDataHelper.getMockMessages(labelId, page, limit);
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
}
