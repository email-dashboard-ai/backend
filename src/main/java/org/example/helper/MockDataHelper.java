package org.example.helper;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MockDataHelper {

  @Getter
  private List<Label> mockLabels;
  private List<Message> mockMessages;

  // Use Google's Factory, not Jackson's ObjectMapper
  private final GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

  @PostConstruct
  public void init() {
    try {
      // Load Labels
      try (InputStream is = new ClassPathResource("mock-data/labels.json").getInputStream()) {
        // parseArray(CollectionClass, ItemClass)
        mockLabels =
            (List<Label>) jsonFactory.createJsonParser(is).parseArray(ArrayList.class, Label.class);
      }

      // Load Emails
      try (InputStream is = new ClassPathResource("mock-data/emails.json").getInputStream()) {
        mockMessages =
            (List<Message>)
                jsonFactory.createJsonParser(is).parseArray(ArrayList.class, Message.class);
      }

      log.info("Mock data loaded successfully: {} emails", mockMessages.size());

    } catch (IOException e) {
      log.error("Failed to load mock data JSON files", e);
      mockLabels = Collections.emptyList();
      mockMessages = Collections.emptyList();
    }
  }

    // Implements Pagination and Filter by Label
  public List<Message> getMockMessages(String labelId, int page, int limit) {
    if (mockMessages == null) return Collections.emptyList();

    // 1. Filter by Label
    List<Message> filtered =
        mockMessages.stream()
            .filter(m -> m.getLabelIds() != null && m.getLabelIds().contains(labelId))
            .collect(Collectors.toList());

    // 2. Calculate Pagination (0-indexed internally)
    int fromIndex = (page - 1) * limit;

    if (fromIndex >= filtered.size()) {
      return Collections.emptyList();
    }

    int toIndex = Math.min(fromIndex + limit, filtered.size());

    return filtered.subList(fromIndex, toIndex);
  }

  public Message getMockMessageDetail(String id) {
    return mockMessages.stream()
        .filter(m -> m.getId().equals(id))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Mock email not found"));
  }
}
