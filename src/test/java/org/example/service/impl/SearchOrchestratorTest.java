package org.example.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.dto.request.SearchRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchOrchestratorTest {

  @InjectMocks private SearchOrchestrator searchOrchestrator;

  @Test
  void resolve_ShouldReturnGmailApi_WhenRequestIsNull() {
    SearchOrchestrator.SearchResult result = searchOrchestrator.resolve(null);
    assertThat(result.getStrategy()).isEqualTo(SearchOrchestrator.Strategy.GMAIL_API);
    assertThat(result.getFuzzyQuery()).isNull();
  }

  @Test
  void resolve_ShouldReturnGmailApi_WhenOnlyGmailFieldsPresent() {
    SearchRequest request =
        SearchRequest.builder().from("test@example.com").subject("Hello").build();

    SearchOrchestrator.SearchResult result = searchOrchestrator.resolve(request);

    assertThat(result.getStrategy()).isEqualTo(SearchOrchestrator.Strategy.GMAIL_API);
    assertThat(result.getGmailQuery()).contains("from:test@example.com");
    assertThat(result.getGmailQuery()).contains("subject:Hello");
    assertThat(result.getFuzzyQuery()).isNull();
  }

  @Test
  void resolve_ShouldReturnGmailApi_WhenOnlyBodyFieldPresent() {
    SearchRequest request = SearchRequest.builder().body("search term").build();

    SearchOrchestrator.SearchResult result = searchOrchestrator.resolve(request);

    assertThat(result.getStrategy()).isEqualTo(SearchOrchestrator.Strategy.GMAIL_API);
    assertThat(result.getGmailQuery()).contains("\"search term\"");
    assertThat(result.getFuzzyQuery()).isNull();
  }

  @Test
  void resolve_ShouldReturnGmailApi_WhenBothFieldsPresent() {
    SearchRequest request =
        SearchRequest.builder().from("test@example.com").body("search term").build();

    SearchOrchestrator.SearchResult result = searchOrchestrator.resolve(request);

    assertThat(result.getStrategy()).isEqualTo(SearchOrchestrator.Strategy.GMAIL_API);
    assertThat(result.getGmailQuery()).contains("from:test@example.com");
    assertThat(result.getGmailQuery()).contains("\"search term\"");
    assertThat(result.getFuzzyQuery()).isNull();
  }
}
