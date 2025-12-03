package org.example.service;

import java.util.List;
import org.example.dto.response.UserPublicProfile;

public interface UserService {
  List<UserPublicProfile> getPublicProfiles(List<String> emails);
}
