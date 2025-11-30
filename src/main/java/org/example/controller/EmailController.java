package org.example.controller;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.config.AppConfig;
import org.example.helper.ResponseWrapper;
import org.example.service.EmailService;
import org.example.dto.response.EmailPageResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gmail")
@RequiredArgsConstructor
@Tag(
    name = "Gmail Operations",
    description = "Endpoints for fetching labels and emails (Mock or Real)")
@SecurityRequirement(name = "bearerAuth") // This applies security to all endpoints in this class
public class EmailController {

  private final EmailService emailService;
  private final AppConfig appConfig;

  @Operation(
      summary = "Get Mailboxes",
      description = "Returns a list of labels (folders) like INBOX, SENT, etc.")
  @GetMapping("/labels")
  public ResponseWrapper<List<Label>> getLabels(@AuthenticationPrincipal UserDetails userDetails) {
    return ResponseWrapper.success(
        emailService.getLabels(userDetails.getUsername()), "Labels fetched successfully");
  }

  @Operation(
      summary = "List Emails",
      description =
          "Returns a paginated list of emails for a specific label. "
              + "Uses pageToken for pagination.")
  @GetMapping("/list/{labelId}")
  public ResponseWrapper<EmailPageResponse> getEmails(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable String labelId,
      @RequestParam(required = false) String pageToken,
      @RequestParam(required = false) Integer limit) {
    int actualLimit = (limit != null) ? limit : appConfig.getGmail().getDefaultLimit();
    actualLimit = Math.min(actualLimit, appConfig.getGmail().getMaxLimit());
    return ResponseWrapper.success(
        emailService.getEmails(userDetails.getUsername(), labelId, pageToken, actualLimit),
        "Emails fetched successfully");
  }

  @Operation(
      summary = "Get Email Detail",
      description = "Returns the full content of a specific email by ID.")
  @GetMapping("/{id}")
  public ResponseWrapper<Message> getEmailDetail(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable String id) {
    return ResponseWrapper.success(
        emailService.getEmailDetails(userDetails.getUsername(), id),
        "Email details fetched successfully");
  }

  @Operation(summary = "Mark Email as Read", description = "Marks an email as read.")
  @PostMapping("/{id}/read")
  public ResponseWrapper<Void> markAsRead(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable String id) {
    emailService.markAsRead(userDetails.getUsername(), id);
    return ResponseWrapper.success(null, "Email marked as read");
  }

  @Operation(summary = "Mark Email as Unread", description = "Marks an email as unread.")
  @PostMapping("/{id}/unread")
  public ResponseWrapper<Void> markAsUnread(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable String id) {
    emailService.markAsUnread(userDetails.getUsername(), id);
    return ResponseWrapper.success(null, "Email marked as unread");
  }

  @Operation(summary = "Toggle Star", description = "Adds or removes star from an email.")
  @PostMapping("/{id}/star")
  public ResponseWrapper<Void> toggleStar(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable String id,
      @RequestParam boolean starred) {
    emailService.toggleStar(userDetails.getUsername(), id, starred);
    return ResponseWrapper.success(null, starred ? "Email starred" : "Email unstarred");
  }

  @Operation(summary = "Delete Email", description = "Moves an email to trash.")
  @DeleteMapping("/{id}")
  public ResponseWrapper<Void> deleteEmail(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable String id) {
    emailService.deleteEmail(userDetails.getUsername(), id);
    return ResponseWrapper.success(null, "Email moved to trash");
  }

  @Operation(summary = "Untrash Email", description = "Restores an email from trash.")
  @PostMapping("/{id}/untrash")
  public ResponseWrapper<Void> untrashEmail(
      @AuthenticationPrincipal UserDetails userDetails, @PathVariable String id) {
    emailService.untrashEmail(userDetails.getUsername(), id);
    return ResponseWrapper.success(null, "Email restored from trash");
  }

  @Operation(summary = "Batch Delete Emails", description = "Moves multiple emails to trash.")
  @PostMapping("/batch/delete")
  public ResponseWrapper<Void> batchDeleteEmails(
      @AuthenticationPrincipal UserDetails userDetails, @RequestBody List<String> ids) {
    emailService.batchDeleteEmails(userDetails.getUsername(), ids);
    return ResponseWrapper.success(null, "Emails moved to trash");
  }

  @Operation(summary = "Batch Mark Read/Unread", description = "Marks multiple emails as read or unread.")
  @PostMapping("/batch/status")
  public ResponseWrapper<Void> batchUpdateStatus(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody List<String> ids,
      @RequestParam boolean isRead) {
    if (isRead) {
      emailService.batchMarkAsRead(userDetails.getUsername(), ids);
    } else {
      emailService.batchMarkAsUnread(userDetails.getUsername(), ids);
    }
    return ResponseWrapper.success(null, "Emails status updated");
  }

  @Operation(summary = "Download Attachment", description = "Downloads an attachment from an email.")
  @GetMapping("/{messageId}/attachments/{attachmentId}")
  public org.springframework.http.ResponseEntity<byte[]> getAttachment(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable String messageId,
      @PathVariable String attachmentId) {
    byte[] data = emailService.getAttachment(userDetails.getUsername(), messageId, attachmentId);
    return org.springframework.http.ResponseEntity.ok()
        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attachment\"")
        .body(data);
  }
}
