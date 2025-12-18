package org.example.dto.request;

import lombok.Data;

@Data
public class AiEmailSummaryRequest {
  private String messageId;
  private String content;
}
