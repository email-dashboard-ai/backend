package org.example.exception;

import org.example.dto.response.ResponseWrapper;
import org.example.enums.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handle Duplicate User Registration (409 Conflict)
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ResponseWrapper<Void>> handleUserExists(UserAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ErrorCode.ERR_USER_EXISTED, ex.getMessage());
    }

    // 2. Handle Login Failures (401 Unauthorized)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ResponseWrapper<Void>> handleBadCredentials(BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCode.ERR_BAD_CREDENTIALS, "Invalid email or password");
    }

    // 3. Handle Token Refresh Issues (403 Forbidden)
    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ResponseWrapper<Void>> handleTokenRefresh(TokenRefreshException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ErrorCode.ERR_TOKEN_REFRESH, ex.getMessage());
    }

    // 4. Handle User Not Found (404 Not Found)
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ResponseWrapper<Void>> handleUserNotFound(UsernameNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ErrorCode.ERR_USER_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseWrapper<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.ERR_DATA_INVALID, "Invalid JSON payload: " + ex.getMessage());
    }

    // 5. Catch-all for other runtime errors (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseWrapper<Void>> handleGenericException(Exception ex) {
        ex.printStackTrace(); // Print log for debugging
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.ERR_SYSTEM, "An unexpected error occurred: " + ex.getMessage());
    }

    private ResponseEntity<ResponseWrapper<Void>> buildResponse(HttpStatus status, ErrorCode errorCode, String message) {
        return new ResponseEntity<>(ResponseWrapper.error(errorCode, message), status);
    }
}