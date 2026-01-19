package org.example.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entity representing a Kanban column configuration. Each user can have multiple custom columns
 * that optionally map to Gmail labels.
 */
@Entity
@Table(
    name = "kanban_columns",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "column_id"}))
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanbanColumn extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** Display name of the column (e.g., "Inbox", "To Do", "Waiting for Reply") */
  @Column(nullable = false, length = 100)
  private String name;

  /** Unique identifier for the column (e.g., "inbox", "todo", "waiting_for_reply") */
  @Column(name = "column_id", nullable = false, length = 50)
  private String columnId;

  /** Display order position (0-based) */
  @Column(nullable = false)
  private Integer position;

  /** Gmail label ID for syncing (null if not mapped to Gmail) */
  @Column(name = "gmail_label_id", length = 100)
  private String gmailLabelId;

  /** Gmail label name (for display purposes) */
  @Column(name = "gmail_label_name", length = 100)
  private String gmailLabelName;

  /** Hex color code for UI display (e.g., "#3B82F6") */
  @Column(nullable = false, length = 7)
  private String color;

  /** Whether this is a system default column (cannot be deleted) */
  @Column(name = "is_default", nullable = false)
  @Builder.Default
  private Boolean isDefault = false;
}
