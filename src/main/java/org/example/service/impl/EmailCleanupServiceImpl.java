package org.example.service.impl;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.repository.SyncedEmailRepository;
import org.example.service.EmailCleanupService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailCleanupServiceImpl implements EmailCleanupService {

  private final SyncedEmailRepository syncedEmailRepository;
  private final org.example.config.AppConfig appConfig;

  /** Daily cleanup of emails older than configured retention period. Runs at midnight every day. */
  @Scheduled(cron = "0 0 0 * * *")
  @Transactional
  public void cleanOldEmails() {
    int retentionDays = appConfig.getSync().getRetentionDays();
    LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
    log.info(
        "Starting daily email cleanup. Deleting emails received before {} (Retention: {} days)",
        cutoff,
        retentionDays);

    syncedEmailRepository.deleteByReceivedDateBefore(cutoff);

    log.info("Daily email cleanup completed.");
  }
}
