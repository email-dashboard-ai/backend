package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.enums.KanbanStatus;
import org.example.helper.ResponseWrapper;
import org.example.service.EmailTaskService;
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

  @Operation(
      summary = "Get Kanban Statuses",
      description = "Returns a map of email IDs to their Kanban status.")
  @GetMapping("/statuses")
  public ResponseWrapper<Map<String, KanbanStatus>> getStatuses(
      @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseWrapper.success(
        emailTaskService.getTaskStatuses(userDetails.getUsername()),
        "Statuses fetched successfully");
  }

  @Operation(
      summary = "Update Email Status",
      description = "Updates the Kanban status of an email.")
  @PostMapping("/status")
  public ResponseWrapper<Void> updateStatus(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestParam String emailId,
      @RequestParam KanbanStatus status) {
    emailTaskService.updateStatus(userDetails.getUsername(), emailId, status);
    return ResponseWrapper.success(null, "Status updated successfully");
  }
}
