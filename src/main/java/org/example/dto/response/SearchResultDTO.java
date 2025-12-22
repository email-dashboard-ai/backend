package org.example.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {
  private String messageId;
  private String subject;
  private String from;
  private String snippet;
  private LocalDateTime receivedDate;
  private String strategy; // GMAIL_API, INTERNAL, or HYBRID

  public static SearchResultDTO fromSyncedEmail(
      org.example.model.SyncedEmail email, String strategy) {
    return SearchResultDTO.builder()
        .messageId(email.getMessageId())
        .subject(email.getSubject())
        .from(email.getFrom())
        .snippet(email.getSnippet())
        .receivedDate(email.getReceivedDate())
        .strategy(strategy)
        .build();
  }

  public static SearchResultDTO fromGmailMessage(
      com.google.api.services.gmail.model.Message message, String strategy) {
    String subject = "";
    String from = "";
    LocalDateTime receivedDate = null;

    if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
      for (var header : message.getPayload().getHeaders()) {
        if ("Subject".equalsIgnoreCase(header.getName())) {
          subject = header.getValue();
        } else if ("From".equalsIgnoreCase(header.getName())) {
          from = header.getValue();
        }
      }
    }

    if (message.getInternalDate() != null) {
      receivedDate = LocalDateTime.ofInstant(
          java.time.Instant.ofEpochMilli(message.getInternalDate()),
          java.time.ZoneId.systemDefault());
    }

    return SearchResultDTO.builder()
        .messageId(message.getId())
        .subject(subject)
        .from(from)
        .snippet(message.getSnippet())
        .receivedDate(receivedDate)
        .strategy(strategy)
        .build();
  }
}
