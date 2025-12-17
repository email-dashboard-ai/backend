package org.example.service.impl;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.example.enums.AuthProvider;
import org.example.model.SnoozedEmail;
import org.example.model.User;
import java.time.Instant;
import org.example.dto.response.EmailPageResponse;
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

  // Constructor Injection automatically finds all implementations of
  // EmailProviderStrategy
  public EmailServiceImpl(UserRepository userRepository, List<EmailProviderStrategy> strategyList,
      SnoozedEmailRepository snoozedEmailRepository) {
    this.userRepository = userRepository;
    this.snoozedEmailRepository = snoozedEmailRepository;

    // Convert list of strategies to a Map for O(1) lookup: { LOCAL -> MockStrategy,
    // GOOGLE ->
    // GoogleStrategy }
    this.strategies = strategyList.stream()
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
    return getStrategy(user).getEmails(user, labelId, pageToken, limit);
  }

  public Message getEmailDetails(String email, String messageId) {
    User user = userRepository.findByEmail(email).orElseThrow();
    return getStrategy(user).getEmailDetails(user, messageId);
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

    User user = userRepository.findByEmail(username)
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
}
