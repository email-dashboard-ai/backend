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
        private int defaultLimit = 50;
    }
}
