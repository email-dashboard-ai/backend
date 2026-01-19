package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.KanbanColumnRequest;
import org.example.dto.response.KanbanColumnResponse;
import org.example.enums.KanbanStatus;
import org.example.helper.ResponseWrapper;
import org.example.service.EmailTaskService;
import org.example.service.KanbanColumnService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kanban")
@RequiredArgsConstructor
@Tag(name = "Kanban Operations", description = "Endpoints for Kanban board management")
@SecurityRequirement(name = "bearerAuth")
public class KanbanController {

  private final EmailTaskService emailTaskService;
  private final KanbanColumnService kanbanColumnService;

  // ============================================================================
  // Column Management
  // ============================================================================

  @Operation(summary = "Get Kanban Columns", description = "Returns all Kanban columns for the current user, ordered by position.")
  @GetMapping("/columns")
  public ResponseWrapper<List<KanbanColumnResponse>> getColumns(
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseWrapper.success(
        kanbanColumnService.getUserColumns(userDetails.getUsername()),
        "Columns fetched successfully");
  }

  @Operation(summary = "Create Kanban Column", description = "Creates a new Kanban column. Automatically creates a Gmail label or links to an existing one with the same name.")
  @PostMapping("/columns")
  public ResponseWrapper<KanbanColumnResponse> createColumn(
      @AuthenticationPrincipal UserDetails userDetails,
      @Valid @RequestBody KanbanColumnRequest request)
      throws IOException {
    return ResponseWrapper.success(
        kanbanColumnService.createColumn(userDetails.getUsername(), request),
        "Column created successfully");
  }

  @Operation(summary = "Update Kanban Column", description = "Updates an existing Kanban column. Renames Gmail label if column is mapped.")
  @PutMapping("/columns/{id}")
  public ResponseWrapper<KanbanColumnResponse> updateColumn(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long id,
      @Valid @RequestBody KanbanColumnRequest request)
      throws IOException {
    return ResponseWrapper.success(
        kanbanColumnService.updateColumn(userDetails.getUsername(), id, request),
        "Column updated successfully");
  }

  @Operation(summary = "Delete Kanban Column", description = "Deletes a Kanban column. Also deletes the Gmail label if column is mapped. Cannot delete default columns.")
  @DeleteMapping("/columns/{id}")
  public ResponseWrapper<Void> deleteColumn(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) throws IOException {
    kanbanColumnService.deleteColumn(userDetails.getUsername(), id);
    return ResponseWrapper.success(null, "Column deleted successfully");
  }

  // ============================================================================
  // Email Status Management
  // ============================================================================

  @Operation(summary = "Get Kanban Statuses", description = "Returns a map of email IDs to their Kanban status.")
  @GetMapping("/statuses")
  public ResponseWrapper<Map<String, KanbanStatus>> getStatuses(
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseWrapper.success(
        emailTaskService.getTaskStatuses(userDetails.getUsername()),
        "Statuses fetched successfully");
  }

  @Operation(summary = "Update Email Status", description = "Updates the Kanban status of an email.")
  @PostMapping("/status")
  public ResponseWrapper<Void> updateStatus(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestParam String emailId,
      @RequestParam KanbanStatus status) {
    emailTaskService.updateStatus(userDetails.getUsername(), emailId, status);
    return ResponseWrapper.success(null, "Status updated successfully");
  }

  @Operation(summary = "Move Email to Column", description = "Moves an email to a target column, updating status and Gmail labels.")
  @PostMapping("/move")
  public ResponseWrapper<Void> moveEmail(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestParam String emailId,
      @RequestParam String targetColumnId) {
    kanbanColumnService.moveEmailToColumn(userDetails.getUsername(), emailId, targetColumnId);
    return ResponseWrapper.success(null, "Email moved successfully");
  }
}
