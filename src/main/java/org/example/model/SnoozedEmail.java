package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = false)
public class SnoozedEmail extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER) // Changed to EAGER to fix scheduler lazy loading issue
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "email_id", nullable = false)
  private String emailId;

  @Column(name = "snoozed_until", nullable = false)
  private Instant snoozedUntil;
}
