package org.example.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Database initialization configuration Ensures pgvector extension is enabled before Hibernate
 * creates tables
 */
@Configuration
@Slf4j
public class DatabaseInitConfig {

  @Bean
  public CommandLineRunner enablePgVectorExtension(JdbcTemplate jdbcTemplate) {
    return args -> {
      try {
        log.info("Enabling pgvector extension...");
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        log.info("pgvector extension enabled successfully");
      } catch (Exception e) {
        log.error(
            "Failed to enable pgvector extension. Please ensure pgvector is installed in your PostgreSQL database.",
            e);
        log.error("Installation guide: https://github.com/pgvector/pgvector#installation");
      }
    };
  }
}
