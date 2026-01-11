package org.example.ai.provider;

import org.example.ai.model.AiProviderId;
import org.example.ai.model.AiSummaryResult;

public interface AiProvider {
  AiProviderId id();

  AiSummaryResult summarize(String inputText);

  /** Summarize with custom prompt (for per-user customization) */
  default AiSummaryResult summarize(String inputText, String customPrompt) {
    // Default implementation uses standard summarize
    // Providers should override this for custom prompt support
    return summarize(inputText);
  }
}
