package org.example.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.enums.AuthProvider;
import org.example.exception.GmailNetworkException;
import org.example.exception.GmailServiceException;
import org.example.model.SnoozedEmail;
import org.example.model.User;
import org.example.repository.SnoozedEmailRepository;
import org.example.service.impl.strategy.EmailProviderStrategy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SnoozeSchedulerService {
  private final SnoozedEmailRepository snoozedEmailRepository;
  private final Map<AuthProvider, EmailProviderStrategy> strategies;

  @Scheduled(fixedRate = 30000)
  @Transactional
  public void wakeUpSnoozeEmail() {
    log.info("Running scheduled task to check for snoozed emails...");
    List<SnoozedEmail> dueEmails = snoozedEmailRepository.findBySnoozedUntilBefore(Instant.now());

    if (dueEmails.isEmpty()) {
      log.debug("No snoozed email to wake up");
      return;
    }

    int successCount = 0;
    int failureCount = 0;

    for (SnoozedEmail snoozedEmail : dueEmails) {
      try {
        wakeUpEmail(snoozedEmail);
        successCount++;

      } catch (GmailServiceException e) {
        log.error("Gmail error waking up email {}: {}", snoozedEmail.getEmailId(), e.getMessage());
        failureCount++;

      } catch (GmailNetworkException e) {
        log.error(
            "Network error waking up email {}: {}", snoozedEmail.getEmailId(), e.getMessage());
        failureCount++;

      } catch (Exception e) {
        log.error(
            "Unexpected error waking up email {}: {}", snoozedEmail.getEmailId(), e.getMessage());
        failureCount++;
      }
    }

    log.info("Woke up {} snoozed emails successfully, {} failed", successCount, failureCount);
  }

  private void wakeUpEmail(SnoozedEmail snoozedEmail) {
    User user = snoozedEmail.getUser();

    EmailProviderStrategy strategy = getStrategy(user);

    String snoozedLabelId = strategy.getSnoozedLabelId(user);

    strategy.modifyLabels(
        user, snoozedEmail.getEmailId(), List.of("INBOX"), List.of(snoozedLabelId));

    snoozedEmailRepository.delete(snoozedEmail);

    log.info("Woke up email {} for user {}", snoozedEmail.getEmailId(), user.getEmail());
  }

  private EmailProviderStrategy getStrategy(User user) {
    EmailProviderStrategy strategy = strategies.get(user.getProvider());

    if (strategy == null) {
      throw new IllegalStateException("No strategy found for provider" + user.getProvider());
    }

    return strategy;
  }
}
