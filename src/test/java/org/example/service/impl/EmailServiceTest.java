package org.example.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import java.time.LocalDateTime;
import java.util.List;
import org.example.config.AppConfig;
import org.example.model.SyncedEmail;
import org.example.repository.SnoozedEmailRepository;
import org.example.repository.UserRepository;
import org.example.service.impl.strategy.EmailProviderStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private SnoozedEmailRepository snoozedEmailRepository;
  @Mock private org.example.repository.SyncedEmailRepository syncedEmailRepository;
  @Mock private AppConfig appConfig;
  @Mock private SearchOrchestrator searchOrchestrator;
  @Mock private EmailProviderStrategy emailProviderStrategy;

  private EmailServiceImpl emailService;

  @BeforeEach
  void setUp() {
    AppConfig.SyncConfig syncConfig = new AppConfig.SyncConfig();
    syncConfig.setRetentionDays(30);
    lenient().when(appConfig.getSync()).thenReturn(syncConfig);

    emailService = new EmailServiceImpl(
        userRepository,
        List.of(emailProviderStrategy),
        snoozedEmailRepository,
        syncedEmailRepository,
        appConfig,
        searchOrchestrator
    );
  }

  @Test
  void toSyncedEmail_ShouldCorrectlyMapGmailMessage() {
    // Given
    Message message = new Message()
        .setId("msg123")
        .setInternalDate(System.currentTimeMillis())
        .setSnippet("Email snippet")
        .setPayload(new MessagePart()
            .setHeaders(List.of(
                new MessagePartHeader().setName("Subject").setValue("Test Subject"),
                new MessagePartHeader().setName("From").setValue("sender@example.com")
            ))
            .setMimeType("text/plain")
            .setBody(new com.google.api.services.gmail.model.MessagePartBody()
                .setData(java.util.Base64.getUrlEncoder().encodeToString("Email body content".getBytes())))
        );

    // When
    SyncedEmail syncedEmail = emailService.toSyncedEmail(message, "user@example.com");

    // Then
    assertThat(syncedEmail.getMessageId()).isEqualTo("msg123");
    assertThat(syncedEmail.getSubject()).isEqualTo("Test Subject");
    assertThat(syncedEmail.getFrom()).isEqualTo("sender@example.com");
    assertThat(syncedEmail.getBody()).isEqualTo("Email body content");
    assertThat(syncedEmail.getSnippet()).isEqualTo("Email snippet");
    assertThat(syncedEmail.getUserEmail()).isEqualTo("user@example.com");
  }

  @Test
  void isWithinRetentionPeriod_ShouldReturnTrue_WhenEmailIsRecent() {
    // Given
    SyncedEmail email = SyncedEmail.builder()
        .receivedDate(LocalDateTime.now().minusDays(5))
        .build();

    // When
    boolean result = emailService.isWithinRetentionPeriod(email);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isWithinRetentionPeriod_ShouldReturnFalse_WhenEmailIsOld() {
    // Given
    SyncedEmail email = SyncedEmail.builder()
        .receivedDate(LocalDateTime.now().minusDays(35))
        .build();

    // When
    boolean result = emailService.isWithinRetentionPeriod(email);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void getHeader_ShouldReturnEmptyString_WhenHeaderNotFound() {
    // Given
    Message message = new Message().setPayload(new MessagePart().setHeaders(List.of()));

    // When
    String subject = emailService.getHeader(message, "Subject");

    // Then
    assertThat(subject).isEmpty();
  }

  @Test
  void getBody_ShouldFallbackToSnippet_WhenBodyIsEmpty() {
    // Given
    Message message = new Message()
        .setSnippet("fallback snippet")
        .setPayload(new MessagePart().setMimeType("text/plain"));

    // When
    String body = emailService.getBody(message);

    // Then
    assertThat(body).isEqualTo("fallback snippet");
  }
}
