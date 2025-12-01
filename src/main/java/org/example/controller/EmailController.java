package org.example.controller;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.config.AppConfig;
import org.example.dto.request.ReplyEmailRequest;
import org.example.dto.request.SendEmailRequest;
import org.example.helper.ResponseWrapper;
import org.example.service.EmailService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/gmail")
@RequiredArgsConstructor
@Tag(name = "Gmail Operations", description = "Endpoints for fetching labels and emails (Mock or Real)")
@SecurityRequirement(name = "bearerAuth") // This applies security to all endpoints in this class
public class EmailController {

  private final EmailService emailService;
  private final AppConfig appConfig;

  @Operation(summary = "Get Mailboxes", description = "Returns a list of labels (folders) like INBOX, SENT, etc.")
  @GetMapping("/labels")
  public ResponseWrapper<List<Label>> getLabels(@AuthenticationPrincipal UserDetails userDetails) {
    return ResponseWrapper.success(
        emailService.getLabels(userDetails.getUsername()), "Labels fetched successfully");
  }

  @Operation(summary = "List Emails", description = "Returns a paginated list of emails for a specific label. "
      + "For Mock users, this supports pagination. For Google users, it maps 'limit' to maxResults.")
  @GetMapping("/list/{labelId}")
  public ResponseWrapper<List<Message>> getEmails(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable String labelId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(required = false) Integer limit) {
    int actualLimit = (limit != null) ? limit : appConfig.getGmail().getDefaultLimit();
    return ResponseWrapper.success(
        emailService.getEmails(userDetails.getUsername(), labelId, page, actualLimit),
        "Emails fetched successfully");
  }

  @Operation(summary = "Get Email Detail", description = "Returns the full content of a specific email by ID.")
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

  @Operation(summary = "Send Email", description = "Sends an email with optional attachments.")
  @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseWrapper<Void> sendEmail(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestPart("data") SendEmailRequest request,
      @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
    emailService.sendEmail(
        userDetails.getUsername(),
        request.getTo(),
        request.getCc(),
        request.getBcc(),
        request.getSubject(),
        request.getBody(),
        attachments);
    return ResponseWrapper.success("Email sent successfully");
  }

  @Operation(summary = "Reply Email", description = "Replies to an email with optional attachments.")
  @PostMapping(value = "/{id}/reply", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseWrapper<Void> replyEmail(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable String id,
      @RequestPart("data") ReplyEmailRequest request,
      @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
    emailService.replyEmail(
        userDetails.getUsername(),
        id,
        request.getTo(),
        request.getCc(),
        request.getBcc(),
        request.getBody(),
        attachments);
    return ResponseWrapper.success("Reply sent successfully");
  }
}
