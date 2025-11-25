package org.example.service.impl.strategy;


import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import lombok.RequiredArgsConstructor;
import org.example.enums.AuthProvider;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleEmailStrategy implements EmailProviderStrategy {

    private final UserRepository userRepository;

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${google.client.secret}")
    private String googleClientSecret;

    @Value("${google.application.name}")
    private String applicationName;

    /**
     * Gets a valid Gmail client.
     * If the access token is invalid/expired, it uses the Refresh Token to get a new one
     * and updates the Database.
     */
    private Gmail getGmailClient(User user) {
        try {
            GoogleCredential credential = new GoogleCredential().setAccessToken(user.getGoogleAccessToken());

            // 1. Build the Gmail service with current token
            Gmail service = new Gmail.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
            ).setApplicationName(applicationName).build();

            // 2. Proactive Check: Try a lightweight call (e.g., get Profile) to check validity
            // Or, simply catch the 401 error in the actual method.
            // Here, we will assume the token is valid, but if we catch an exception below, we refresh.
            return service;

        } catch (Exception e) {
            throw new RuntimeException("Error building Gmail client", e);
        }
    }

    /**
     * Helper to refresh token and save to DB
     */
    private String refreshAccessToken(User user) throws IOException {
        if (user.getGoogleRefreshToken() == null) {
            throw new RuntimeException("No Google Refresh Token available for user " + user.getEmail());
        }

        GoogleTokenResponse response = new GoogleRefreshTokenRequest(
                new NetHttpTransport(),
                new GsonFactory(),
                user.getGoogleRefreshToken(),
                googleClientId,
                googleClientSecret
        ).execute();

        String newAccessToken = response.getAccessToken();

        // Save to DB immediately so subsequent calls use the new one
        user.setGoogleAccessToken(newAccessToken);
        userRepository.save(user);

        return newAccessToken;
    }

    @Override
    public List<Label> getLabels(User user) {
        try {
            return executeGetLabels(user);
        } catch (Exception e) {
            // If error is 401 Unauthorized, refresh and retry once
            if (e.getMessage().contains("401") || e.getMessage().contains("Invalid Credentials")) {
                try {
                    refreshAccessToken(user);
                    return executeGetLabels(user); // Retry with new token from updated user object
                } catch (IOException | GeneralSecurityException ioException) {
                    throw new RuntimeException("Failed to refresh token", ioException);
                }
            }
            throw new RuntimeException("Gmail API Error", e);
        }
    }

    private List<Label> executeGetLabels(User user) throws IOException, java.security.GeneralSecurityException {
        Gmail service = getGmailClient(user);
        return service.users().labels().list("me").execute().getLabels();
    }

    @Override
    public List<Message> getEmails(User user, String labelId, int page, int limit) {
        try {
            return executeGetEmails(user, labelId, limit);
        } catch (Exception e) {
            if (e.getMessage().contains("401") || e.getMessage().contains("Invalid Credentials")) {
                try {
                    refreshAccessToken(user);
                    return executeGetEmails(user, labelId, limit);
                } catch (IOException ioException) {
                    throw new RuntimeException("Failed to refresh token", ioException);
                } catch (GeneralSecurityException ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw new RuntimeException("Gmail API Error", e);
        }
    }

    private List<Message> executeGetEmails(User user, String labelId, int limit) throws IOException, java.security.GeneralSecurityException {
        Gmail service = getGmailClient(user);

        // Convert user provided limit to Long
        long maxResults = (long) limit;

        var response = service.users().messages().list("me")
                .setLabelIds(List.of(labelId))
                .setMaxResults(maxResults)
                .execute();

        List<Message> messages = new ArrayList<>();
        if (response.getMessages() != null) {
            for (Message msg : response.getMessages()) {
                messages.add(service.users().messages().get("me", msg.getId()).setFormat("full").execute());
            }
        }
        return messages;
    }

    @Override
    public Message getEmailDetails(User user, String messageId) {
        try {
            return executeGetDetail(user, messageId);
        } catch (Exception e) {
            if (e.getMessage().contains("401") || e.getMessage().contains("Invalid Credentials")) {
                try {
                    refreshAccessToken(user);
                    return executeGetDetail(user, messageId);
                } catch (IOException ioException) {
                    throw new RuntimeException("Failed to refresh token", ioException);
                }
            }
            throw e;
        }
    }

    private Message executeGetDetail(User user, String messageId) {
        try {
            Gmail service = getGmailClient(user);
            return service.users().messages().get("me", messageId).execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AuthProvider getSupportedProvider() {
        return AuthProvider.GOOGLE;
    }
}