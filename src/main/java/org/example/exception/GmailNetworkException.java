package org.example.exception;

import java.io.IOException;

public class GmailNetworkException extends RuntimeException {
  public GmailNetworkException(IOException ex) {
    super("Network error communicating with Gmail ApI", ex);
  }
}
