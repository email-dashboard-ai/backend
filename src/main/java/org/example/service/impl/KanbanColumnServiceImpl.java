package org.example.service.impl;

import com.google.api.services.gmail.model.Label;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.request.KanbanColumnRequest;
import org.example.dto.response.KanbanColumnResponse;
import org.example.enums.KanbanStatus;
import org.example.exception.ResourceNotFoundException;
import org.example.exception.ValidationException;
import org.example.model.KanbanColumn;
import org.example.model.User;
import org.example.repository.KanbanColumnRepository;
import org.example.repository.UserRepository;
import org.example.service.EmailTaskService;
import org.example.service.KanbanColumnService;
import org.example.service.impl.strategy.GoogleEmailStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KanbanColumnServiceImpl implements KanbanColumnService {

  private final KanbanColumnRepository kanbanColumnRepository;
  private final UserRepository userRepository;
  private final GoogleEmailStrategy googleEmailStrategy;
  private final EmailTaskService emailTaskService; // Injected dependency

  @Override
  @Transactional(readOnly = true)
  public List<KanbanColumnResponse> getUserColumns(String userEmail) {
    User user = findUserByEmail(userEmail);
    List<KanbanColumn> columns = kanbanColumnRepository.findByUserOrderByPositionAsc(user);

    return columns.stream().map(this::toResponse).collect(Collectors.toList());
  }

  @Override
  @Transactional
  public KanbanColumnResponse createColumn(String userEmail, KanbanColumnRequest request)
      throws IOException {
    User user = findUserByEmail(userEmail);

    // Validate column ID is unique for this user
    if (kanbanColumnRepository.existsByUserAndColumnId(user, request.columnId())) {
      throw new ValidationException(
          "Column with ID '"
              + request.columnId()
              + "' already exists. Please choose a different name or delete the existing column first.");
    }

    // Determine position (append to end if not specified)
    int position = request.position() != null ? request.position() : getNextPosition(user);

    // Create column entity
    KanbanColumn column =
        KanbanColumn.builder()
            .user(user)
            .name(request.name())
            .columnId(request.columnId())
            .position(position)
            .color(request.color())
            .isDefault(false)
            .build();

    // ALWAYS create/link Gmail label (forced sync for all columns)
    try {
      // createGmailLabel will check if label exists and return it, or create new one
      Label gmailLabel = googleEmailStrategy.createGmailLabel(user, request.name());
      column.setGmailLabelId(gmailLabel.getId());
      column.setGmailLabelName(gmailLabel.getName());
      log.info(
          "Linked Gmail label '{}' (ID: {}) for column '{}'",
          gmailLabel.getName(),
          gmailLabel.getId(),
          request.columnId());
    } catch (IOException e) {
      log.error(
          "Failed to create/link Gmail label for column '{}': {}",
          request.columnId(),
          e.getMessage());
      throw new IOException("Failed to create/link Gmail label: " + e.getMessage(), e);
    }

    column = kanbanColumnRepository.save(column);
    log.info(
        "Created Kanban column '{}' for user '{}' with Gmail sync",
        column.getColumnId(),
        userEmail);

    return toResponse(column);
  }

  @Override
  @Transactional
  public KanbanColumnResponse updateColumn(
      String userEmail, Long columnId, KanbanColumnRequest request) throws IOException {
    User user = findUserByEmail(userEmail);
    KanbanColumn column =
        kanbanColumnRepository
            .findByUserAndId(user, columnId)
            .orElseThrow(() -> new ResourceNotFoundException("Column not found"));

    // Prevent updating default columns' IDs
    if (column.getIsDefault() && !column.getColumnId().equals(request.columnId())) {
      throw new ValidationException("Cannot change ID of default column");
    }

    // Update basic fields
    boolean nameChanged = !column.getName().equals(request.name());
    column.setName(request.name());
    column.setColor(request.color());

    if (request.position() != null) {
      column.setPosition(request.position());
    }

    // If name changed and column has Gmail label, rename the label
    if (nameChanged && column.getGmailLabelId() != null) {
      try {
        Label updatedLabel =
            googleEmailStrategy.renameGmailLabel(user, column.getGmailLabelId(), request.name());
        column.setGmailLabelName(updatedLabel.getName());
        log.info(
            "Renamed Gmail label to '{}' for column '{}'",
            updatedLabel.getName(),
            column.getColumnId());
      } catch (IOException e) {
        log.error("Failed to rename Gmail label for column '{}'", column.getColumnId(), e);
        throw new IOException("Failed to rename Gmail label: " + e.getMessage(), e);
      }
    }

    column = kanbanColumnRepository.save(column);
    log.info("Updated Kanban column '{}' for user '{}'", column.getColumnId(), userEmail);

    return toResponse(column);
  }

  @Override
  @Transactional
  public void deleteColumn(String userEmail, Long columnId) throws IOException {
    User user = findUserByEmail(userEmail);
    KanbanColumn column =
        kanbanColumnRepository
            .findByUserAndId(user, columnId)
            .orElseThrow(() -> new ResourceNotFoundException("Column not found"));

    // Prevent deleting default columns
    if (column.getIsDefault()) {
      throw new ValidationException("Cannot delete default column");
    }

    // TODO: Check if column has emails - prevent deletion if it does
    // This requires querying EmailTask repository

    // Delete Gmail label if it exists
    if (column.getGmailLabelId() != null) {
      try {
        googleEmailStrategy.deleteGmailLabel(user, column.getGmailLabelId());
        log.info(
            "Deleted Gmail label '{}' for column '{}'",
            column.getGmailLabelName(),
            column.getColumnId());
      } catch (IOException e) {
        log.error("Failed to delete Gmail label for column '{}'", column.getColumnId(), e);
        // Continue with column deletion even if Gmail label deletion fails
      }
    }

    kanbanColumnRepository.delete(column);
    log.info("Deleted Kanban column '{}' for user '{}'", column.getColumnId(), userEmail);
  }

  @Override
  @Transactional
  public void initializeDefaultColumns(String userEmail) {
    log.info(
        "Skipping default column initialization for user '{}' to avoid duplicate key issues. Columns should be created manually or via frontend requirements.",
        userEmail);
  }

  // Helper methods

  private User findUserByEmail(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }

  private int getNextPosition(User user) {
    List<KanbanColumn> columns = kanbanColumnRepository.findByUserOrderByPositionAsc(user);
    return columns.isEmpty() ? 0 : columns.get(columns.size() - 1).getPosition() + 1;
  }

  private KanbanColumnResponse toResponse(KanbanColumn column) {
    return new KanbanColumnResponse(
        column.getId(),
        column.getName(),
        column.getColumnId(),
        column.getPosition(),
        column.getGmailLabelId(),
        column.getGmailLabelName(),
        column.getColor(),
        column.getIsDefault());
  }

  @Override
  @Transactional
  public void moveEmailToColumn(String userEmail, String emailId, String targetColumnId) {
    User user = findUserByEmail(userEmail);

    // 1. Find target column (ALL columns must have Gmail labels now)
    KanbanColumn targetColumn =
        kanbanColumnRepository
            .findByUserAndColumnId(user, targetColumnId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Column not found: " + targetColumnId));

    // 2. Determine KanbanStatus (for local DB tracking)
    KanbanStatus status;
    String normalizedId = targetColumnId.toLowerCase();

    if ("inbox".equals(normalizedId)) status = KanbanStatus.INBOX;
    else if ("todo".equals(normalizedId)) status = KanbanStatus.TO_DO;
    else if ("in_progress".equals(normalizedId)) status = KanbanStatus.IN_PROGRESS;
    else if ("done".equals(normalizedId)) status = KanbanStatus.DONE;
    else status = KanbanStatus.IN_PROGRESS; // Default for custom columns

    // 3. Update local database status
    emailTaskService.updateStatus(userEmail, emailId, status);

    // 4. Sync Gmail Labels
    // If the user has custom columns mapped to Gmail labels, update them.
    try {
      List<String> labelsToAdd = new ArrayList<>();
      List<String> labelsToRemove = new ArrayList<>();

      // Get all columns with Gmail labels
      List<KanbanColumn> allColumns = kanbanColumnRepository.findByUserOrderByPositionAsc(user);

      log.info(
          "📧 Moving email {} to column '{}' (ID: {})",
          emailId,
          targetColumn.getName(),
          targetColumnId);

      // Validate that target column has a Gmail label (required now)
      if (targetColumn.getGmailLabelId() == null) {
        throw new ValidationException(
            "Target column '"
                + targetColumn.getName()
                + "' has no Gmail label. All columns must be synced with Gmail.");
      }

      // Add target column's label
      labelsToAdd.add(targetColumn.getGmailLabelId());
      log.info(
          "  ✓ Will ADD label: '{}' ({})", targetColumn.getName(), targetColumn.getGmailLabelId());

      // Remove ALL other Kanban column labels to ensure clean "move"
      for (KanbanColumn col : allColumns) {
        if (!col.getColumnId().equals(targetColumnId) && col.getGmailLabelId() != null) {
          labelsToRemove.add(col.getGmailLabelId());
          log.info("  ✗ Will REMOVE label: '{}' ({})", col.getName(), col.getGmailLabelId());
        }
      }

      // Execute label modification
      if (!labelsToAdd.isEmpty() || !labelsToRemove.isEmpty()) {
        log.info("🔄 Calling Gmail API - ADD: {}, REMOVE: {}", labelsToAdd, labelsToRemove);
        googleEmailStrategy.modifyLabels(user, emailId, labelsToAdd, labelsToRemove);
        log.info("✅ Successfully modified Gmail labels for email {}", emailId);
      } else {
        log.warn("⚠️  No label changes needed for email {} - this shouldn't happen", emailId);
      }

    } catch (Exception e) {
      log.error("❌ Failed to sync Gmail labels for email {}: {}", emailId, e.getMessage(), e);
      // Log but don't fail the transaction, as local update succeeded
    }
  }
}
