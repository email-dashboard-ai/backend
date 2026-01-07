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
   * or body. Returns results ordered by relevance (similarity).
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
              + "  OR unaccent(e.subject) % unaccent(:query) "
              + "  OR unaccent(e.sender) % unaccent(:query) "
              + "  OR unaccent(e.body) % unaccent(:query)"
              + ") "
              + "ORDER BY "
              + "  GREATEST("
              + "    similarity(unaccent(e.subject), unaccent(:query)), "
              + "    similarity(unaccent(e.body), unaccent(:query))"
              + "  ) DESC, "
              + "  e.received_date DESC",
      nativeQuery = true)
  List<SyncedEmail> searchEmails(
      @Param("userEmail") String userEmail, @Param("query") String query);

  void deleteByReceivedDateBefore(LocalDateTime cutoff);
}
