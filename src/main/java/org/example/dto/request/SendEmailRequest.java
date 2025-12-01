package org.example.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class SendEmailRequest {
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String body;
}
