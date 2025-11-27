package org.example.service.impl.strategy;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import java.util.List;
import org.example.enums.AuthProvider;
import org.example.model.User;

public interface EmailProviderStrategy {
  List<Label> getLabels(User user);

  List<Message> getEmails(User user, String labelId, int page, int limit);

  Message getEmailDetails(User user, String messageId);

  AuthProvider getSupportedProvider();
}
