package org.example.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "email_summaries",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"message_id", "user_email", "content_hash"})
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSummary {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "message_id", nullable = false)
  private String messageId;

  @Column(name = "user_email", nullable = false)
  private String userEmail;

  @Column(name = "content_hash", nullable = false, length = 64)
  private String contentHash;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String summary;

  @Column(nullable = false, length = 50)
  private String provider;

  @Column(nullable = false, length = 100)
  private String model;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
