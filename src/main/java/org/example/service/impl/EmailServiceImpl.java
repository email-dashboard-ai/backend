package org.example.service.impl;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.example.enums.AuthProvider;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.service.EmailService;
import org.example.service.impl.strategy.EmailProviderStrategy;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
  private final UserRepository userRepository;
  private final Map<AuthProvider, EmailProviderStrategy> strategies;

  // Constructor Injection automatically finds all implementations of EmailProviderStrategy
  public EmailServiceImpl(UserRepository userRepository, List<EmailProviderStrategy> strategyList) {
    this.userRepository = userRepository;
    // Convert list of strategies to a Map for O(1) lookup: { LOCAL -> MockStrategy, GOOGLE ->
    // GoogleStrategy }
    this.strategies =
        strategyList.stream()
            .collect(
                Collectors.toMap(EmailProviderStrategy::getSupportedProvider, Function.identity()));
  }

  private EmailProviderStrategy getStrategy(User user) {
    return strategies.get(user.getProvider());
  }

  public List<Label> getLabels(String email) {
    User user = userRepository.findByEmail(email).orElseThrow();
    return getStrategy(user).getLabels(user);
  }

  public List<Message> getEmails(String email, String labelId, int page, int limit) {
    User user = userRepository.findByEmail(email).orElseThrow();
    return getStrategy(user).getEmails(user, labelId, page, limit);
  }

  public Message getEmailDetails(String email, String messageId) {
    User user = userRepository.findByEmail(email).orElseThrow();
    return getStrategy(user).getEmailDetails(user, messageId);
  }
}
