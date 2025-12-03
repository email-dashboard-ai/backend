package org.example.helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.enums.ErrorCode;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseWrapper<T> {
  private boolean success;
  private int errorCode;
  private String message;
  private T data;

  public static <T> ResponseWrapper<T> success(T data, String message) {
    return ResponseWrapper.<T>builder()
        .success(true)
        .errorCode(ErrorCode.SUCCESS.getCode())
        .message(message)
        .data(data)
        .build();
  }

  public static <T> ResponseWrapper<T> success(String message) {
    return ResponseWrapper.<T>builder()
        .success(true)
        .errorCode(ErrorCode.SUCCESS.getCode())
        .message(message)
        .build();
  }

  public static <T> ResponseWrapper<T> error(ErrorCode errorCode, String message) {
    return ResponseWrapper.<T>builder()
        .success(false)
        .errorCode(errorCode.getCode())
        .message(message != null ? message : errorCode.getMessage())
        .build();
  }
}
