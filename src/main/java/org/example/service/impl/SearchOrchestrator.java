package org.example.service.impl;

import lombok.Getter;
import org.example.dto.request.SearchRequest;
import org.springframework.stereotype.Component;

/**
 * Strategy: GMAIL_API | INTERNAL | HYBRID - GMAIL_API: only Gmail fields (from, to, subject, etc.)
 * - INTERNAL: only body field (fuzzy search) - HYBRID: Gmail fields + body → Gmail filters first,
 * then fuzzy on results
 */
@Component
public class SearchOrchestrator {

  public enum Strategy {
    GMAIL_API,
    INTERNAL,
    HYBRID
  }

  @Getter
  public static class SearchResult {
    private final Strategy strategy;
    private final String gmailQuery;
    private final String fuzzyQuery;

    public SearchResult(Strategy strategy, String gmailQuery, String fuzzyQuery) {
      this.strategy = strategy;
      this.gmailQuery = gmailQuery;
      this.fuzzyQuery = fuzzyQuery;
    }
  }

  /**
   * Determines search strategy based on request fields: - GMAIL_API: Default for all searches
   * (fastest). Used for Gmail fields and simple body search. - INTERNAL: Only when
   * useFuzzySearch=true and only body field is used (typo tolerance). - HYBRID: Only when
   * useFuzzySearch=true AND Gmail fields + body are used.
   */
  public SearchResult resolve(SearchRequest request) {
    if (request == null) {
      return new SearchResult(Strategy.GMAIL_API, "", null);
    }

    boolean hasGmailFields = request.hasGmailFields();
    boolean hasFuzzyFields = request.hasFuzzyFields(); // body + useFuzzySearch=true
    boolean hasBodySearch = request.hasBodySearch(); // body without fuzzy

    // Case 1: Fuzzy search explicitly enabled with Gmail filters -> HYBRID
    if (hasFuzzyFields && hasGmailFields) {
      return new SearchResult(Strategy.HYBRID, request.toGmailQuery(), request.getBody());
    }

    // Case 2: Fuzzy search explicitly enabled, body only -> INTERNAL
    if (hasFuzzyFields) {
      return new SearchResult(Strategy.INTERNAL, null, request.getBody());
    }

    // Case 3: DEFAULT - Use Gmail API for everything (Gmail fields and/or body search)
    // This is 6-10x faster than INTERNAL strategy
    if (hasGmailFields || hasBodySearch) {
      return new SearchResult(Strategy.GMAIL_API, request.toGmailQuery(), null);
    }

    // Case 4: Empty search
    return new SearchResult(Strategy.GMAIL_API, "", null);
  }
}
