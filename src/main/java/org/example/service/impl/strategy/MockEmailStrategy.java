package org.example.service.impl.strategy;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import lombok.RequiredArgsConstructor;
import org.example.enums.AuthProvider;
import org.example.helper.MockDataHelper;
import org.example.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MockEmailStrategy implements EmailProviderStrategy {

    private final MockDataHelper mockDataHelper;

    @Override
    public List<Label> getLabels(User user) {
        return mockDataHelper.getMockLabels();
    }

    @Override
    public List<Message> getEmails(User user, String labelId, int page, int limit) {
        return mockDataHelper.getMockMessages(labelId, page, limit);
    }

    @Override
    public Message getEmailDetails(User user, String messageId) {
        return mockDataHelper.getMockMessageDetail(messageId);
    }

    @Override
    public AuthProvider getSupportedProvider() {
        return AuthProvider.LOCAL;
    }
}