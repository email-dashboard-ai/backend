package org.example.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

  // Gmail API fields → strategy: GMAIL_API or HYBRID
  private String from;
  private String to;
  private String cc;
  private String bcc;
  private String subject;
  private String filename;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate after;

  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate before;

  private String label;
  private String category;
  private Boolean hasAttachment;
  private Boolean isUnread;
  private Boolean isStarred;
  private Boolean isRead;
  private Boolean isImportant;

  // Fuzzy search field → strategy: INTERNAL or HYBRID (only when useFuzzySearch=true)
  private String body;

  // When true, uses PostgreSQL trigram for typo-tolerant search (slower)
  // When false/null, uses Gmail API for exact body text search (faster)
  private Boolean useFuzzySearch;

  private static final DateTimeFormatter GMAIL_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy/MM/dd");

  public boolean hasGmailFields() {
    return isNotBlank(from)
        || isNotBlank(to)
        || isNotBlank(cc)
        || isNotBlank(bcc)
        || isNotBlank(subject)
        || isNotBlank(filename)
        || after != null
        || before != null
        || isNotBlank(label)
        || isNotBlank(category)
        || hasAttachment != null
        || isUnread != null
        || isStarred != null
        || isRead != null
        || isImportant != null;
  }

  /**
   * Returns true only if user wants fuzzy/typo-tolerant search. Requires both body field AND
   * useFuzzySearch=true.
   */
  public boolean hasFuzzyFields() {
    return isNotBlank(body) && Boolean.TRUE.equals(useFuzzySearch);
  }

  /**
   * Returns true if there's body text for simple (exact) search. Used when useFuzzySearch is not
   * enabled.
   */
  public boolean hasBodySearch() {
    return isNotBlank(body) && !Boolean.TRUE.equals(useFuzzySearch);
  }

  public String toGmailQuery() {
    StringBuilder query = new StringBuilder();

    if (isNotBlank(from)) query.append("from:").append(from).append(" ");
    if (isNotBlank(to)) query.append("to:").append(to).append(" ");
    if (isNotBlank(cc)) query.append("cc:").append(cc).append(" ");
    if (isNotBlank(bcc)) query.append("bcc:").append(bcc).append(" ");
    if (isNotBlank(subject)) query.append("subject:").append(subject).append(" ");
    if (isNotBlank(filename)) query.append("filename:").append(filename).append(" ");
    if (after != null) query.append("after:").append(after.format(GMAIL_DATE_FORMAT)).append(" ");
    if (before != null)
      query.append("before:").append(before.format(GMAIL_DATE_FORMAT)).append(" ");
    if (isNotBlank(label)) query.append("label:").append(label).append(" ");
    if (isNotBlank(category)) query.append("category:").append(category).append(" ");
    if (hasAttachment != null && hasAttachment) query.append("has:attachment ");
    if (isUnread != null && isUnread) query.append("is:unread ");
    if (isRead != null && isRead) query.append("is:read ");
    if (isStarred != null && isStarred) query.append("is:starred ");
    if (isImportant != null && isImportant) query.append("is:important ");

    // Include body text for Gmail search (when not using fuzzy)
    if (isNotBlank(body) && !Boolean.TRUE.equals(useFuzzySearch)) {
      // Flexible keyword search (matches any occurrence of keywords, like Gmail Web UI)
      query.append(body).append(" ");
    }

    return query.toString().trim();
  }

  private boolean isNotBlank(String s) {
    return s != null && !s.isBlank();
  }
}
