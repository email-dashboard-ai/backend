package org.example.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AiEmailSummaryResponse {
  String messageId;
  String summary;
  String provider;
  String model;
  boolean cached;
  long latencyMs;
}
