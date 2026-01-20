package org.example.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.example.model.SyncedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncedEmailRepository extends JpaRepository<SyncedEmail, String> {

  /**
   * Fuzzy search using PostgreSQL pg_trgm and unaccent. Matches against subject, sender, snippet,
   * or body. Uses word_similarity() for better partial word matching - this finds similar words
   * within longer text (e.g., "rnder" matches "Render" in "Render <no-reply@render.com>").
   */
  @Query(
      value =
          "SELECT * FROM synced_emails e "
              + "WHERE e.user_email = :userEmail "
              + "AND ("
              + "  unaccent(e.subject) ILIKE unaccent('%' || :query || '%') "
              + "  OR unaccent(e.sender) ILIKE unaccent('%' || :query || '%') "
              + "  OR unaccent(e.snippet) ILIKE unaccent('%' || :query || '%') "
              + "  OR unaccent(e.body) ILIKE unaccent('%' || :query || '%') "
              + "  OR word_similarity(unaccent(:query), unaccent(e.subject)) > 0.3 "
              + "  OR word_similarity(unaccent(:query), unaccent(e.sender)) > 0.3 "
              + "  OR word_similarity(unaccent(:query), unaccent(e.body)) > 0.3"
              + ") "
              + "ORDER BY "
              + "  GREATEST("
              + "    word_similarity(unaccent(:query), unaccent(e.subject)), "
              + "    word_similarity(unaccent(:query), unaccent(e.sender)), "
              + "    word_similarity(unaccent(:query), unaccent(e.body))"
              + "  ) DESC, "
              + "  e.received_date DESC",
      nativeQuery = true)
  List<SyncedEmail> searchEmails(
      @Param("userEmail") String userEmail, @Param("query") String query);

  /**
   * Semantic search using vector similarity (cosine distance)
   * Finds emails most similar to the query embedding
   * 
   * @param userEmail User's email address
   * @param queryEmbedding Query embedding as string representation of vector
   * @param limit Maximum number of results to return
   * @return List of similar emails ordered by similarity (most similar first)
   */
  @Query(
      value =
          "SELECT * FROM synced_emails e "
              + "WHERE e.user_email = :userEmail "
              + "AND e.embedding IS NOT NULL "
              + "ORDER BY e.embedding <=> CAST(:queryEmbedding AS vector) "
              + "LIMIT :limit",
      nativeQuery = true)
  List<SyncedEmail> semanticSearch(
      @Param("userEmail") String userEmail,
      @Param("queryEmbedding") String queryEmbedding,
      @Param("limit") int limit);

  /**
   * Find emails without embeddings for a specific user
   * Used for background generation of embeddings
   * 
   * @param userEmail User's email address
   * @return List of emails that need embeddings
   */
  @Query(
      value =
          "SELECT * FROM synced_emails e "
              + "WHERE e.user_email = :userEmail "
              + "AND e.embedding IS NULL "
              + "ORDER BY e.received_date DESC",
      nativeQuery = true)
  List<SyncedEmail> findEmailsWithoutEmbeddings(@Param("userEmail") String userEmail);

  void deleteByReceivedDateBefore(LocalDateTime cutoff);
}
