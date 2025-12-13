package org.example.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.enums.KanbanStatus;

@Entity
@Table(name = "email_tasks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"email_id", "user_id"})
})
@Data
@NoArgsConstructor
public class EmailTask {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "email_id", nullable = false)
  private String emailId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private KanbanStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
}
