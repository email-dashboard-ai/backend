package org.example.repository;

import java.time.Instant;
import java.util.List;

import org.example.model.SnoozedEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnoozedEmailRepository extends JpaRepository<SnoozedEmail, Long> {
    List<SnoozedEmail> findBySnoozedUntilBefore(Instant snoozedUntil);
}
