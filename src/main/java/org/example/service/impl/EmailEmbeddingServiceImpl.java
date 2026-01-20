package org.example.service.impl;

import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.SyncedEmail;
import org.example.repository.SyncedEmailRepository;
import org.example.service.EmailEmbeddingService;
import org.example.service.EmbeddingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service implementation for generating embeddings for emails
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailEmbeddingServiceImpl implements EmailEmbeddingService {

  private final SyncedEmailRepository syncedEmailRepository;
  private final EmbeddingService embeddingService;

  @Override
  @Transactional
  public int generateMissingEmbeddings(String userEmail) {
    log.info("Generating embeddings for user: {}", userEmail);
    
    // First, let's see how many total emails this user has
    long totalEmails = syncedEmailRepository.count();
    log.info("Total emails in database: {}", totalEmails);
    
    List<SyncedEmail> emailsWithoutEmbeddings = 
        syncedEmailRepository.findEmailsWithoutEmbeddings(userEmail);
    
    log.info("Query returned {} emails without embeddings for user: {}", 
        emailsWithoutEmbeddings.size(), userEmail);
    
    if (emailsWithoutEmbeddings.isEmpty()) {
      log.warn("No emails without embeddings found for user: {}. Either all have embeddings or user has no emails.", userEmail);
      return 0;
    }
    
    log.info("Found {} emails without embeddings for user: {}", 
        emailsWithoutEmbeddings.size(), userEmail);
    
    int processed = 0;
    for (SyncedEmail email : emailsWithoutEmbeddings) {
      try {
        PGvector embedding = embeddingService.generateEmailEmbedding(
            email.getSubject(),
            email.getFrom(),
            email.getBody()
        );
        
        if (embedding != null) {
          email.setEmbedding(embedding);
          email.setEmbeddingGeneratedAt(LocalDateTime.now());
          syncedEmailRepository.save(email);
          processed++;
          
          if (processed % 10 == 0) {
            log.debug("Generated embeddings for {}/{} emails", 
                processed, emailsWithoutEmbeddings.size());
          }
        } else {
          log.warn("Failed to generate embedding for email: {}", email.getMessageId());
        }
        
      } catch (Exception e) {
        log.error("Error generating embedding for email {}: {}", 
            email.getMessageId(), e.getMessage());
      }
    }
    
    log.info("Generated embeddings for {}/{} emails for user: {}", 
        processed, emailsWithoutEmbeddings.size(), userEmail);
    
    return processed;
  }

  @Override
  @Transactional
  public int generateAllMissingEmbeddings() {
    log.info("Starting batch embedding generation for all users");
    
    // This could be optimized by getting distinct users first
    List<SyncedEmail> allEmailsWithoutEmbeddings = 
        syncedEmailRepository.findAll().stream()
            .filter(email -> email.getEmbedding() == null)
            .toList();
    
    if (allEmailsWithoutEmbeddings.isEmpty()) {
      log.info("No emails without embeddings across all users");
      return 0;
    }
    
    log.info("Found {} emails without embeddings across all users", 
        allEmailsWithoutEmbeddings.size());
    
    int processed = 0;
    for (SyncedEmail email : allEmailsWithoutEmbeddings) {
      try {
        PGvector embedding = embeddingService.generateEmailEmbedding(
            email.getSubject(),
            email.getFrom(),
            email.getBody()
        );
        
        if (embedding != null) {
          email.setEmbedding(embedding);
          email.setEmbeddingGeneratedAt(LocalDateTime.now());
          syncedEmailRepository.save(email);
          processed++;
          
          if (processed % 50 == 0) {
            log.info("Generated embeddings for {}/{} emails", 
                processed, allEmailsWithoutEmbeddings.size());
          }
        }
        
      } catch (Exception e) {
        log.error("Error generating embedding for email {}: {}", 
            email.getMessageId(), e.getMessage());
      }
    }
    
    log.info("Completed batch embedding generation: {}/{} emails processed", 
        processed, allEmailsWithoutEmbeddings.size());
    
    return processed;
  }
}
