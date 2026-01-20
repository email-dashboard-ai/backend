package org.example.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchRequest {
  
  @NotBlank(message = "Query is required")
  private String query;
  
  @Min(value = 1, message = "Limit must be at least 1")
  private int limit = 20; // Default to 20 results
}
