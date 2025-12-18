package org.example.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.example.model.SnoozedEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnoozedEmailRepository extends JpaRepository<SnoozedEmail, Long> {
  List<SnoozedEmail> findBySnoozedUntilBefore(Instant snoozedUntil);

  Optional<SnoozedEmail> findByEmailIdAndUserEmail(String emailId, String userEmail);

  List<SnoozedEmail> findByUserEmail(String userEmail);
}
