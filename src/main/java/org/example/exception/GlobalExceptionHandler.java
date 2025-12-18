package org.example.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.ai.exception.AiException;
import org.example.enums.ErrorCode;
import org.example.helper.ResponseWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ResponseWrapper<Void>> handleUserExists(UserAlreadyExistsException ex) {
    return buildResponse(HttpStatus.CONFLICT, ErrorCode.ERR_USER_EXISTED, ex.getMessage());
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ResponseWrapper<Void>> handleBadCredentials(BadCredentialsException ex) {
    return buildResponse(
        HttpStatus.UNAUTHORIZED, ErrorCode.ERR_BAD_CREDENTIALS, "Invalid email or password");
  }

  @ExceptionHandler(TokenRefreshException.class)
  public ResponseEntity<ResponseWrapper<Void>> handleTokenRefresh(TokenRefreshException ex) {
    return buildResponse(HttpStatus.FORBIDDEN, ErrorCode.ERR_TOKEN_REFRESH, ex.getMessage());
  }

  @ExceptionHandler(UsernameNotFoundException.class)
  public ResponseEntity<ResponseWrapper<Void>> handleUserNotFound(UsernameNotFoundException ex) {
    return buildResponse(HttpStatus.NOT_FOUND, ErrorCode.ERR_USER_NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ResponseWrapper<Void>> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex) {
    return buildResponse(
        HttpStatus.BAD_REQUEST,
        ErrorCode.ERR_DATA_INVALID,
        "Invalid JSON payload: " + ex.getMessage());
  }

  @ExceptionHandler(GmailServiceException.class)
  public ResponseEntity<ResponseWrapper<Void>> handleGoogleException(GmailServiceException ex) {
    var googleEx = ex.getGoogleException();
    int statusCode = googleEx.getStatusCode();

    ErrorCode errorCode;
    String message;
    HttpStatus httpStatus;

    switch (statusCode) {
      case 400:
        errorCode = ErrorCode.ERR_GMAIL_INVALID_REQUEST;
        message = "Invalid Gmail API request. Please check your input.";
        httpStatus = HttpStatus.BAD_REQUEST;
        break;

      case 401:
        errorCode = ErrorCode.ERR_GMAIL_AUTH_EXPIRED;
        message = "Gmail authentication expired. Please sign in again.";
        httpStatus = HttpStatus.UNAUTHORIZED;
        break;

      case 403:
        errorCode = ErrorCode.ERR_GMAIL_PERMISSION_DENIED;
        message = "Insufficient permissions to access Gmail. Please grant required permissions.";
        httpStatus = HttpStatus.FORBIDDEN;
        break;

      case 404:
        errorCode = ErrorCode.ERR_GMAIL_NOT_FOUND;
        message = "Gmail resource not found. The email or label may have been deleted.";
        httpStatus = HttpStatus.NOT_FOUND;
        break;

      case 409:
        errorCode = ErrorCode.ERR_GMAIL_LABEL_EXIST;
        message = "Gmail label already exists";
        httpStatus = HttpStatus.CONFLICT;
        break;

      case 429:
        errorCode = ErrorCode.ERR_GMAIL_QUOTA_EXCEEDED;
        message = "Gmail API quota exceeded. Please try again later.";
        httpStatus = HttpStatus.TOO_MANY_REQUESTS;
        break;

      case 500:

      case 503:
        errorCode = ErrorCode.ERR_GMAIL_SERVICE;
        message = "Gmail service temporarily unavailable.";
        httpStatus = HttpStatus.valueOf(statusCode);
        break;

      default:
        errorCode = ErrorCode.ERR_GMAIL_SERVICE;
        message =
            googleEx.getDetails() != null
                ? googleEx.getDetails().getMessage()
                : googleEx.getMessage();
        httpStatus = HttpStatus.valueOf(statusCode);
    }

    log.error("Gmail service error ({}): {}", httpStatus, message);

    return buildResponse(httpStatus, errorCode, message);
  }

  @ExceptionHandler(GmailNetworkException.class)
  public ResponseEntity<ResponseWrapper<Void>> handleGmailNetworkException(
      GmailNetworkException ex) {
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.ERR_GMAIL_SERVICE,
        "Network error communicating with Gmail. Please check your connection.");
  }

  @ExceptionHandler(AiException.class)
  public ResponseEntity<ResponseWrapper<Void>> handleAiException(AiException ex) {
    if (ex.getCause() != null) {
      log.warn(
          "AI error [{} {}]: {} (cause: {})",
          ex.getHttpStatus(),
          ex.getErrorCode(),
          ex.getMessage(),
          ex.getCause().toString());
    } else {
      log.warn("AI error [{} {}]: {}", ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage());
    }
    return buildResponse(ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ResponseWrapper<Void>> handleGenericException(Exception ex) {
    log.error("Unexpected error: ", ex);
    return buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.ERR_SYSTEM,
        "An unexpected error occurred: " + ex.getMessage());
  }

  private ResponseEntity<ResponseWrapper<Void>> buildResponse(
      HttpStatus status, ErrorCode errorCode, String message) {
    return new ResponseEntity<>(ResponseWrapper.error(errorCode, message), status);
  }
}
