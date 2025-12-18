package org.example.ai.provider;

import org.example.ai.config.AiConfig;
import org.example.ai.exception.AiException;
import org.example.ai.model.AiProviderId;
import org.example.ai.model.AiSummaryResult;
import org.example.enums.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiProviderRouter {
  private final AiConfig aiConfig;
  private final Map<AiProviderId, AiProvider> providers;

  public AiProviderRouter(AiConfig aiConfig, List<AiProvider> providerList) {
    this.aiConfig = aiConfig;
    Map<AiProviderId, AiProvider> map = new EnumMap<>(AiProviderId.class);
    for (AiProvider provider : providerList) {
      map.put(provider.id(), provider);
    }
    this.providers = map;
  }

  public AiSummaryResult summarize(String inputText) {
    AiProviderId providerId = parseProvider(aiConfig.getProvider());
    AiProvider provider = providers.get(providerId);
    if (provider == null) {
      throw new AiException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          ErrorCode.ERR_AI_CONFIG,
          "AI provider not configured: " + providerId);
    }
    return provider.summarize(inputText);
  }

  private AiProviderId parseProvider(String value) {
    if (value == null || value.isBlank()) {
      return AiProviderId.GEMINI;
    }
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    try {
      return AiProviderId.valueOf(normalized);
    } catch (IllegalArgumentException ex) {
      throw new AiException(
          HttpStatus.BAD_REQUEST,
          ErrorCode.ERR_AI_REQUEST_INVALID,
          "Unsupported ai.provider: " + value);
    }
  }
}
