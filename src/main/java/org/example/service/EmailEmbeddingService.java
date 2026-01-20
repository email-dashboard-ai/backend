package org.example.service;

/**
 * Service to generate embeddings for emails that don't have them yet
 */
public interface EmailEmbeddingService {
  
  /**
   * Generate embeddings for all emails without embeddings for a specific user
   * 
   * @param userEmail User's email address
   * @return Number of emails processed
   */
  int generateMissingEmbeddings(String userEmail);
  
  /**
   * Generate embeddings for all emails across all users (batch job)
   * 
   * @return Number of emails processed
   */
  int generateAllMissingEmbeddings();
}
