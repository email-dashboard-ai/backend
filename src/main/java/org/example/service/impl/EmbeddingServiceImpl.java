package org.example.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import lombok.extern.slf4j.Slf4j;
import org.example.service.EmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of EmbeddingService using Google's Generative AI API
 * Uses text-embedding-004 model which generates 768-dimensional embeddings
 */
@Service
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {

  private final String apiKey;
  private final String modelName;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public EmbeddingServiceImpl(
      @Value("${google.ai.api-key}") String apiKey,
      @Value("${google.ai.embedding-model}") String modelName
  ) {
    this.apiKey = apiKey;
    this.modelName = modelName;
    this.restTemplate = new RestTemplate();
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public PGvector generateEmbedding(String text) {
    if (text == null || text.trim().isEmpty()) {
      log.warn("Cannot generate embedding for null or empty text");
      return null;
    }

    try {
      // Truncate text if too long (API has limits)
      String truncatedText = text.length() > 10000 ? text.substring(0, 10000) : text;
      
      // Prepare request
      String url = String.format(
          "https://generativelanguage.googleapis.com/v1beta/models/%s:embedContent?key=%s",
          modelName, apiKey
      );
      
      // Build request body
      Map<String, Object> requestBody = new HashMap<>();
      Map<String, Object> content = new HashMap<>();
      Map<String, String> parts = new HashMap<>();
      parts.put("text", truncatedText);
      content.put("parts", List.of(parts));
      requestBody.put("content", content);
      
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      
      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
      
      // Make API call
      ResponseEntity<String> response = restTemplate.exchange(
          url,
          HttpMethod.POST,
          entity,
          String.class
      );
      
      // Parse response
      JsonNode root = objectMapper.readTree(response.getBody());
      JsonNode embeddingNode = root.path("embedding").path("values");
      
      if (!embeddingNode.isArray()) {
        log.error("Invalid response from embedding API");
        return null;
      }
      
      // Convert to float array
      List<Float> embeddingValues = new ArrayList<>();
      for (JsonNode value : embeddingNode) {
        embeddingValues.add((float) value.asDouble());
      }
      
      float[] embeddingArray = new float[embeddingValues.size()];
      for (int i = 0; i < embeddingValues.size(); i++) {
        embeddingArray[i] = embeddingValues.get(i);
      }
      
      log.debug("Generated embedding with {} dimensions", embeddingArray.length);
      return new PGvector(embeddingArray);
      
    } catch (Exception e) {
      log.error("Error generating embedding: {}", e.getMessage(), e);
      return null;
    }
  }

  @Override
  public PGvector generateEmailEmbedding(String subject, String from, String body) {
    // Combine email fields into a single text for embedding
    // Weight subject more heavily as it's often more informative
    StringBuilder combinedText = new StringBuilder();
    
    if (subject != null && !subject.isEmpty()) {
      combinedText.append("Subject: ").append(subject).append("\n\n");
    }
    
    if (from != null && !from.isEmpty()) {
      combinedText.append("From: ").append(from).append("\n\n");
    }
    
    if (body != null && !body.isEmpty()) {
      combinedText.append("Body: ").append(body);
    }
    
    return generateEmbedding(combinedText.toString());
  }
}
