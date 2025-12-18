package org.example.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {
  private String provider;
  private int timeoutMs;
  private int maxInputChars;
  private String summaryPrompt = "You are an email assistant. Summarize the following email in 1-2 concise sentences for a Kanban task card. Focus on: the main action required or key information, who is involved, and any deadlines if mentioned. Ignore greetings, signatures, and promotional content. Be direct and actionable.";

  private Cache cache = new Cache();
  private Gemini gemini = new Gemini();
  private Groq groq = new Groq();

  @Data
  public static class Cache {
    private int ttlSeconds = 86400;
    private int maxEntries = 2000;
  }

  @Data
  public static class Gemini {
    private String apiKey;
    private String model = "gemini-2.5-flash";
    private int maxOutputTokens = 120;
    private double temperature = 0.2;
  }

  @Data
  public static class Groq {
    private String apiKey;
    private String model = "llama-3.3-70b-versatile";
    private int maxOutputTokens = 120;
    private double temperature = 0.2;
  }
}
