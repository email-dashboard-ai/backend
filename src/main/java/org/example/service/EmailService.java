package org.example.service;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import java.time.Instant;
import java.util.List;
import org.example.dto.response.EmailPageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface EmailService {
  List<Label> getLabels(String email);

  EmailPageResponse getEmails(String email, String labelId, String pageToken, int limit);

  Message getEmailDetails(String email, String messageId);

  void markAsRead(String email, String messageId);

  void markAsUnread(String email, String messageId);

  void toggleStar(String email, String messageId, boolean starred);

  void deleteEmail(String email, String messageId);

  void untrashEmail(String email, String messageId);

  void batchDeleteEmails(String email, List<String> messageIds);

  void batchMarkAsRead(String email, List<String> messageIds);

  void batchMarkAsUnread(String email, List<String> messageIds);

  void sendEmail(
      String email,
      List<String> to,
      List<String> cc,
      List<String> bcc,
      String subject,
      String body,
      List<MultipartFile> attachments);

  void replyEmail(
      String email,
      String messageId,
      List<String> to,
      List<String> cc,
      List<String> bcc,
      String body,
      List<MultipartFile> attachments);

  byte[] getAttachment(String username, String messageId, String attachmentId);

  List<Message> getThreadMessages(String username, String threadId);

  void snoozeEmail(String username, String emailId, Instant snoozedUntil);

  void unsnoozeEmail(String username, String emailId);

  java.util.Map<String, Instant> getSnoozedEmailsInfo(String username);

  java.util.List<org.example.dto.response.SearchResultDTO> search(
      String email, org.example.dto.request.SearchRequest request);

  java.util.List<org.example.dto.response.SearchResultDTO> semanticSearch(
      String email, org.example.dto.request.SemanticSearchRequest request);

  void moveToInbox(String email, String messageId);

  void permanentlyDelete(String email, String messageId);
}
