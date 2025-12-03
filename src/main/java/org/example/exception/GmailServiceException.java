package org.example.exception;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import lombok.Getter;

@Getter
public class GmailServiceException extends RuntimeException {
  private final GoogleJsonResponseException googleException;

  public GmailServiceException(GoogleJsonResponseException googleException) {
    super(googleException.getMessage(), googleException);
    this.googleException = googleException;
  }
}
