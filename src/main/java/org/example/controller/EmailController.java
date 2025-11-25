package org.example.controller;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gmail")
@RequiredArgsConstructor
@Tag(name = "Gmail Operations", description = "Endpoints for fetching labels and emails (Mock or Real)")
@SecurityRequirement(name = "bearerAuth") // This applies security to all endpoints in this class
public class EmailController {

    private final EmailService emailService;

    @Operation(summary = "Get Mailboxes", description = "Returns a list of labels (folders) like INBOX, SENT, etc.")
    @GetMapping("/labels")
    public ResponseEntity<List<Label>> getLabels(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(emailService.getLabels(userDetails.getUsername()));
    }

    @Operation(
            summary = "List Emails",
            description = "Returns a paginated list of emails for a specific label. " +
                    "For Mock users, this supports pagination. For Google users, it maps 'limit' to maxResults."
    )
    @GetMapping("/list/{labelId}")
    public ResponseEntity<List<Message>> getEmails(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String labelId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(
                emailService.getEmails(userDetails.getUsername(), labelId, page, limit)
        );
    }

    @Operation(summary = "Get Email Detail", description = "Returns the full content of a specific email by ID.")
    @GetMapping("/{id}")
    public ResponseEntity<Message> getEmailDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id) {
        return ResponseEntity.ok(emailService.getEmailDetails(userDetails.getUsername(), id));
    }
}