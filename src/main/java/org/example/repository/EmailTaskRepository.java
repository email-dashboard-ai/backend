package org.example.repository;

import org.example.model.EmailTask;
import org.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTaskRepository extends JpaRepository<EmailTask, Long> {
  Optional<EmailTask> findByUserAndEmailId(User user, String emailId);
  List<EmailTask> findByUser(User user);
}
