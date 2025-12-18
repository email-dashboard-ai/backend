package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class SendEmailRequest {
  @NotEmpty(message = "At least one recipient is required")
  private List<String> to;

  private List<String> cc;
  private List<String> bcc;

  @NotBlank(message = "Subject is required")
  private String subject;

  @NotBlank(message = "Body is required")
  private String body;
}
