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

  public SearchResult resolve(SearchRequest request) {
    if (request == null) {
      return new SearchResult(Strategy.INTERNAL, null, "");
    }

    boolean hasGmailFields = request.hasGmailFields();
    boolean hasFuzzyFields = request.hasFuzzyFields();

    if (hasGmailFields && hasFuzzyFields) {
      return new SearchResult(Strategy.HYBRID, request.toGmailQuery(), request.getBody());
    } else if (hasGmailFields) {
      return new SearchResult(Strategy.GMAIL_API, request.toGmailQuery(), null);
    } else if (hasFuzzyFields) {
      return new SearchResult(Strategy.INTERNAL, null, request.getBody());
    } else {
      return new SearchResult(Strategy.INTERNAL, null, "");
    }
  }
}
