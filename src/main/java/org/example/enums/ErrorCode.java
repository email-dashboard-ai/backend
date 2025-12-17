package org.example.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {
  SUCCESS(1, "Success"),

  // Common Errors
  ERR_SYSTEM(1000, "System error"),

  ERR_DATA_INVALID(1002, "Data invalid"),

  // Auth Errors

  ERR_USER_EXISTED(4001, "User already exists"),
  ERR_BAD_CREDENTIALS(4002, "Invalid email or password"),
  ERR_TOKEN_REFRESH(4003, "Token refresh failed"),
  ERR_USER_NOT_FOUND(4004, "User not found"),

  // Service Errors
  ERR_GMAIL_SERVICE(6000, "Gmail service error"),
  ERR_GMAIL_AUTH_EXPIRED(6001, "Gmail authentication expired"),
  ERR_GMAIL_PERMISSION_DENIED(6002, "Insufficient Gmail permissions"),
  ERR_GMAIL_QUOTA_EXCEEDED(6003, "Gmail quota exceeded"),
  ERR_GMAIL_LABEL_EXIST(6004, "Gmail label already exists"),
  ERR_GMAIL_NOT_FOUND(6005, "Gmail resource not found"),
  ERR_GMAIL_NETWORK(6006, "Network error communication with Gmail"),
  ERR_GMAIL_INVALID_REQUEST(6007, "Invalid Gmail apI request");

  private final int code;
  private final String message;

  ErrorCode(int code, String message) {
    this.code = code;
    this.message = message;
  }
}
