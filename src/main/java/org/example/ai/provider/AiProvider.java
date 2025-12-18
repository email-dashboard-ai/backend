package org.example.ai.provider;

import org.example.ai.model.AiProviderId;
import org.example.ai.model.AiSummaryResult;

public interface AiProvider {
  AiProviderId id();

  AiSummaryResult summarize(String inputText);
}
