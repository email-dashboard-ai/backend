package org.example.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabaseExtensionInitializer implements CommandLineRunner {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(String... args) {
    try {
      log.info("Attempting to enable PostgreSQL extensions...");
      jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
      jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS unaccent");
      log.info("PostgreSQL extensions enabled successfully.");
    } catch (Exception e) {
      log.warn(
          "Failed to enable PostgreSQL extensions. "
              + "Ensure the database user has superuser permissions or extensions are already installed. "
              + "Error: {}",
          e.getMessage());
    }
  }
}
