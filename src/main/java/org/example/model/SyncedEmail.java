package org.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "synced_emails")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncedEmail {

  @Id
  @Column(name = "message_id")
  private String messageId;

  @Column(name = "user_email", nullable = false)
  private String userEmail;

  @Column(columnDefinition = "TEXT")
  private String subject;

  @Column(name = "sender", columnDefinition = "TEXT")
  private String from;

  @Column(columnDefinition = "TEXT")
  private String snippet;

  @Column(columnDefinition = "TEXT")
  private String body; // Plain text content for searching

  @Column(name = "received_date")
  private LocalDateTime receivedDate;
}
