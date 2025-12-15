package org.example.ai.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AiSummaryResult {
  String summary;
  String provider;
  String model;
  boolean cached;
  long latencyMs;
}
