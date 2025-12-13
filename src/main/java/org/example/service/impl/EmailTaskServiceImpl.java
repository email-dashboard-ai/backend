package org.example.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.enums.KanbanStatus;
import org.example.model.EmailTask;
import org.example.model.User;
import org.example.repository.EmailTaskRepository;
import org.example.repository.UserRepository;
import org.example.service.EmailTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailTaskServiceImpl implements EmailTaskService {

  private final EmailTaskRepository emailTaskRepository;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public void updateStatus(String email, String emailId, KanbanStatus status) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found"));

    Optional<EmailTask> existingTaskWrapper = emailTaskRepository.findByUserAndEmailId(user, emailId);

    if (status == KanbanStatus.INBOX) {
      // If moving back to Inbox, remove the tracking entry (default state)
      existingTaskWrapper.ifPresent(emailTaskRepository::delete);
    } else {
      EmailTask task = existingTaskWrapper.orElse(new EmailTask());
      if (task.getId() == null) {
        task.setUser(user);
        task.setEmailId(emailId);
      }
      task.setStatus(status);
      emailTaskRepository.save(task);
    }
  }

  @Override
  public Map<String, KanbanStatus> getTaskStatuses(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found"));
    List<EmailTask> tasks = emailTaskRepository.findByUser(user);
    
    Map<String, KanbanStatus> statusMap = new HashMap<>();
    for (EmailTask task : tasks) {
      statusMap.put(task.getEmailId(), task.getStatus());
    }
    return statusMap;
  }
}
