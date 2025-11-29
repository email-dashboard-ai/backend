package org.example.service;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import java.util.List;

public interface EmailService {
  List<Label> getLabels(String email);

  List<Message> getEmails(String email, String labelId, int page, int limit);

  Message getEmailDetails(String email, String messageId);

  void markAsRead(String email, String messageId);

  void markAsUnread(String email, String messageId);

  void toggleStar(String email, String messageId, boolean starred);

  void deleteEmail(String email, String messageId);

  void untrashEmail(String email, String messageId);
}
