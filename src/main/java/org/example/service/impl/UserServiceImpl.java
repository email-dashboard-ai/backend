package org.example.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.ai.config.AiConfig;
import org.example.dto.request.UpdateUserSettingsRequest;
import org.example.dto.response.UserPublicProfile;
import org.example.dto.response.UserSettingsResponse;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final AiConfig aiConfig;

  @Override
  public List<UserPublicProfile> getPublicProfiles(List<String> emails) {
    List<User> users = userRepository.findByEmailIn(emails);
    return users.stream()
        .map(
            user ->
                UserPublicProfile.builder()
                    .email(user.getEmail())
                    .name(user.getName())
                    .avatar(user.getAvatar())
                    .build())
        .collect(Collectors.toList());
  }

  @Override
  public UserSettingsResponse getUserSettings(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found: " + email));

    String customPrompt = user.getCustomSummaryPrompt();
    boolean usingCustom = customPrompt != null && !customPrompt.isBlank();

    return UserSettingsResponse.builder()
        .email(user.getEmail())
        .customSummaryPrompt(customPrompt)
        .defaultPrompt(aiConfig.getSummaryPrompt())
        .usingCustomPrompt(usingCustom)
        .build();
  }

  @Override
  @Transactional
  public UserSettingsResponse updateUserSettings(String email, UpdateUserSettingsRequest request) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found: " + email));

    // Set custom prompt (null or empty string will clear it)
    String newPrompt = request.getCustomSummaryPrompt();
    if (newPrompt != null && newPrompt.isBlank()) {
      newPrompt = null; // Convert empty string to null
    }
    user.setCustomSummaryPrompt(newPrompt);
    userRepository.save(user);

    boolean usingCustom = newPrompt != null;

    return UserSettingsResponse.builder()
        .email(user.getEmail())
        .customSummaryPrompt(newPrompt)
        .defaultPrompt(aiConfig.getSummaryPrompt())
        .usingCustomPrompt(usingCustom)
        .build();
  }
}
