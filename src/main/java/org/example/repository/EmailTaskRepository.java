package org.example.repository;

import java.util.List;
import java.util.Optional;
import org.example.enums.KanbanStatus;
import org.example.model.EmailTask;
import org.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailTaskRepository extends JpaRepository<EmailTask, Long> {
  Optional<EmailTask> findByUserAndEmailId(User user, String emailId);
  List<EmailTask> findByUserAndStatus(User user, KanbanStatus status);
  List<EmailTask> findByUser(User user);
}
