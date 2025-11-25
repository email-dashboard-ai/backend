package org.example.service;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;

import java.util.List;

public interface EmailService {
    List<Label> getLabels(String email);

    List<Message> getEmails(String email, String labelId, int page, int limit);

    Message getEmailDetails(String email, String messageId);
}
