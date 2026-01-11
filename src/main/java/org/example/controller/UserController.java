package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.UpdateUserSettingsRequest;
import org.example.dto.response.UserPublicProfile;
import org.example.dto.response.UserSettingsResponse;
import org.example.helper.ResponseWrapper;
import org.example.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

  @Operation(
      summary = "Get User Settings",
      description = "Retrieves the current user's settings including custom AI summary prompt.")
  @GetMapping("/settings")
  public ResponseWrapper<UserSettingsResponse> getUserSettings(Principal principal) {
    return ResponseWrapper.success(
        userService.getUserSettings(principal.getName()), "User settings fetched successfully");
  }

  @Operation(
      summary = "Update User Settings",
      description = "Updates the current user's settings. Set customSummaryPrompt to null or empty to use default prompt.")
  @PutMapping("/settings")
  public ResponseWrapper<UserSettingsResponse> updateUserSettings(
      Principal principal,
      @RequestBody UpdateUserSettingsRequest request) {
    return ResponseWrapper.success(
        userService.updateUserSettings(principal.getName(), request),
        "User settings updated successfully");
  }
}
