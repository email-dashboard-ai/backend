package org.example.exception;

import org.example.helper.ResponseWrapper;
import org.example.enums.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.example.exception.GmailServiceException;

import lombok.extern.slf4j.Slf4j;

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
    return buildResponse(
        HttpStatus.valueOf(googleEx.getStatusCode()),
        ErrorCode.ERR_GMAIL_SERVICE,
        googleEx.getDetails() != null ? googleEx.getDetails().getMessage() : googleEx.getMessage());
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
