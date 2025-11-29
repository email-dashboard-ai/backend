package org.example.service.impl.strategy;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import java.util.List;
import org.example.enums.AuthProvider;
import org.example.model.User;

public interface EmailProviderStrategy {
  List<Label> getLabels(User user);

  List<Message> getEmails(User user, String labelId, int page, int limit);

  Message getEmailDetails(User user, String messageId);

  void markAsRead(User user, String messageId);

  void markAsUnread(User user, String messageId);

  void toggleStar(User user, String messageId, boolean starred);

  void deleteEmail(User user, String messageId);

  void untrashEmail(User user, String messageId);

  void batchDeleteEmails(User user, List<String> messageIds);

  void batchMarkAsRead(User user, List<String> messageIds);

  void batchMarkAsUnread(User user, List<String> messageIds);

  AuthProvider getSupportedProvider();
}
