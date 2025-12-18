package org.example.repository;

import java.util.Optional;
import org.example.model.EmailSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailSummaryRepository extends JpaRepository<EmailSummary, Long> {

  Optional<EmailSummary> findByMessageIdAndUserEmailAndContentHash(
      String messageId, String userEmail, String contentHash);
}
