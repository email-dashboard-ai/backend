package org.example.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(1, "Success"),

    // Common Errors
    ERR_SYSTEM(1000, "System error"),
    ERR_NOT_FOUND(1001, "Data not found"),
    ERR_DATA_INVALID(1002, "Data invalid"),

    // Auth Errors
    ERR_AUTH(4000, "Authenticate fail"),
    ERR_USER_EXISTED(4001, "User already exists"),
    ERR_BAD_CREDENTIALS(4002, "Invalid email or password"),
    ERR_TOKEN_REFRESH(4003, "Token refresh failed"),
    ERR_USER_NOT_FOUND(4004, "User not found"),

    // Service Errors
    ERR_GMAIL_SERVICE(6000, "Gmail service error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
