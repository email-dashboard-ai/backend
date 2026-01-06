package org.example.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.example.enums.AuthProvider;
import org.example.model.SnoozedEmail;
import org.example.model.User;
import org.example.repository.SnoozedEmailRepository;
import org.example.service.impl.strategy.EmailProviderStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SnoozeSchedulerServiceTest {

  @Mock private SnoozedEmailRepository snoozedEmailRepository;
  @Mock private EmailProviderStrategy emailProviderStrategy;

  private SnoozeSchedulerService snoozeSchedulerService;

  @BeforeEach
  void setUp() {
    Map<AuthProvider, EmailProviderStrategy> strategies =
        Map.of(AuthProvider.GOOGLE, emailProviderStrategy);
    snoozeSchedulerService = new SnoozeSchedulerService(snoozedEmailRepository, strategies);
  }

  @Test
  void wakeUpSnoozeEmail_ShouldProcessDueEmails() {
    // Given
    User user = new User();
    user.setProvider(AuthProvider.GOOGLE);
    user.setEmail("test@gmail.com");

    SnoozedEmail email = new SnoozedEmail();
    email.setEmailId("msg123");
    email.setUser(user);
    email.setSnoozedUntil(Instant.now().minusSeconds(60));

    when(snoozedEmailRepository.findBySnoozedUntilBefore(any())).thenReturn(List.of(email));
    when(emailProviderStrategy.getSnoozedLabelId(user)).thenReturn("SNOOZE_LABEL");

    // When
    snoozeSchedulerService.wakeUpSnoozeEmail();

    // Then
    verify(emailProviderStrategy)
        .modifyLabels(eq(user), eq("msg123"), eq(List.of("INBOX")), eq(List.of("SNOOZE_LABEL")));
    verify(snoozedEmailRepository).delete(email);
  }

  @Test
  void wakeUpSnoozeEmail_ShouldContinueProcessing_WhenOneEmailFails() {
    // Given
    User user = new User();
    user.setProvider(AuthProvider.GOOGLE);

    SnoozedEmail email1 = new SnoozedEmail();
    email1.setEmailId("msg1");
    email1.setUser(user);

    SnoozedEmail email2 = new SnoozedEmail();
    email2.setEmailId("msg2");
    email2.setUser(user);

    when(snoozedEmailRepository.findBySnoozedUntilBefore(any()))
        .thenReturn(List.of(email1, email2));

    // Fail first email, succeed second
    doThrow(new RuntimeException("API Error")).when(emailProviderStrategy).getSnoozedLabelId(user);
    // Note: Since both emails share the same user and stubbing is on user, it will fail for both
    // unless we mock differently. Let's make them separate users or mock leniently.

    // Better: Mock modifyLabels to fail for email1
    reset(emailProviderStrategy);
    when(emailProviderStrategy.getSnoozedLabelId(user)).thenReturn("SNOOZE_LABEL");
    doThrow(new RuntimeException("API Error"))
        .when(emailProviderStrategy)
        .modifyLabels(eq(user), eq("msg1"), any(), any());

    // When
    snoozeSchedulerService.wakeUpSnoozeEmail();

    // Then
    verify(snoozedEmailRepository).delete(email2);
    verify(snoozedEmailRepository, never()).delete(email1);
  }
}
