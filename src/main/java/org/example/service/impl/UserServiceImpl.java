package org.example.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.dto.response.UserPublicProfile;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.service.UserService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;

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
}
