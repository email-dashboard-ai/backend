package org.example.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class ReplyEmailRequest {
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String body;
}
