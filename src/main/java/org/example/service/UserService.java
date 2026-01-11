package org.example.service;

import java.util.List;
import org.example.dto.request.UpdateUserSettingsRequest;
import org.example.dto.response.UserPublicProfile;
import org.example.dto.response.UserSettingsResponse;

public interface UserService {
  List<UserPublicProfile> getPublicProfiles(List<String> emails);

  UserSettingsResponse getUserSettings(String email);

  UserSettingsResponse updateUserSettings(String email, UpdateUserSettingsRequest request);
}
