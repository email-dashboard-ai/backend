package org.example.dto.response;

import com.google.api.services.gmail.model.Message;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailPageResponse {
  private List<Message> messages;
  private String nextPageToken;
}
