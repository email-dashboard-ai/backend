package org.example.service;

import java.util.Map;
import org.example.enums.KanbanStatus;

public interface EmailTaskService {
  void updateStatus(String email, String emailId, KanbanStatus status);

  Map<String, KanbanStatus> getTaskStatuses(String email);
}
