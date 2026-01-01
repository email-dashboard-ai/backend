package org.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
  private GmailConfig gmail = new GmailConfig();

  @Data
  public static class GmailConfig {
    private int defaultLimit;
    private int maxLimit;
  }

  private GoogleConfig google = new GoogleConfig();

  @Data
  public static class GoogleConfig {
    private String tokenUri;
    private String redirectUri;

    private int connectTimeoutMs;
    private int readTimeoutMs;
  }

  private SyncConfig sync = new SyncConfig();

  @Data
  public static class SyncConfig {
    private int retentionDays;
  }
}
