package org.example.ai.exception;

import lombok.Getter;
import org.example.enums.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
public class AiException extends RuntimeException {
  private final ErrorCode errorCode;
  private final HttpStatus httpStatus;

  public AiException(HttpStatus httpStatus, ErrorCode errorCode, String message) {
    super(message);
    this.httpStatus = httpStatus;
    this.errorCode = errorCode;
  }

  public AiException(HttpStatus httpStatus, ErrorCode errorCode, String message, Throwable cause) {
    super(message, cause);
    this.httpStatus = httpStatus;
    this.errorCode = errorCode;
  }
}
