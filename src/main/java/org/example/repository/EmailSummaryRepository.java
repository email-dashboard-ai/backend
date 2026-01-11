package org.example.repository;

import java.util.Optional;
import org.example.model.EmailSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface EmailSummaryRepository extends JpaRepository<EmailSummary, Long> {

  Optional<EmailSummary> findByMessageIdAndUserEmailAndContentHash(
      String messageId, String userEmail, String contentHash);

  /**
   * Insert a new email summary, ignoring duplicates (race condition safe). Uses PostgreSQL's ON
   * CONFLICT DO NOTHING to handle concurrent inserts gracefully.
   */
  @Modifying
  @Transactional
  @Query(
      value =
          "INSERT INTO email_summaries (message_id, user_email, content_hash, summary, provider, model, created_at) "
              + "VALUES (:messageId, :userEmail, :contentHash, :summary, :provider, :model, NOW()) "
              + "ON CONFLICT (message_id, user_email, content_hash) DO NOTHING",
      nativeQuery = true)
  void insertIgnoreDuplicate(
      @Param("messageId") String messageId,
      @Param("userEmail") String userEmail,
      @Param("contentHash") String contentHash,
      @Param("summary") String summary,
      @Param("provider") String provider,
      @Param("model") String model);

  /**
   * Delete all summaries for a specific message and user.
   * Used when regenerating summaries to clear old cached versions.
   */
  @Modifying
  @Transactional
  @Query("DELETE FROM EmailSummary e WHERE e.messageId = :messageId AND e.userEmail = :userEmail")
  void deleteByMessageIdAndUserEmail(
      @Param("messageId") String messageId,
      @Param("userEmail") String userEmail);
}
