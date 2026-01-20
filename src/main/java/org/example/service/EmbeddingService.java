package org.example.service;

import com.pgvector.PGvector;

/**
 * Service for generating text embeddings using Google's Generative AI API
 */
public interface EmbeddingService {
  
  /**
   * Generate an embedding vector for the given text
   * 
   * @param text The text to generate an embedding for
   * @return PGvector containing the embedding (768 dimensions)
   */
  PGvector generateEmbedding(String text);
  
  /**
   * Generate an embedding for an email by combining its subject, from, and body
   * 
   * @param subject Email subject
   * @param from Email sender
   * @param body Email body content
   * @return PGvector containing the embedding
   */
  PGvector generateEmailEmbedding(String subject, String from, String body);
}
