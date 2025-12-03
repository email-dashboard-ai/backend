package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.dto.response.UserPublicProfile;
import org.example.helper.ResponseWrapper;
import org.example.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Operations", description = "Endpoints for user management and profile retrieval")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private final UserService userService;

  @Operation(
      summary = "Get Batch User Profiles",
      description = "Fetches public profiles (name, avatar) for a list of email addresses.")
  @PostMapping("/batch-info")
  public ResponseWrapper<List<UserPublicProfile>> getBatchUserProfiles(
      @RequestBody List<String> emails) {
    return ResponseWrapper.success(
        userService.getPublicProfiles(emails), "User profiles fetched successfully");
  }
}
