package org.example.util;

/**
 * Thread-local storage for request context data. Stores the request ID for the current request
 * thread.
 */
public final class RequestContext {

  private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

  private RequestContext() {
    // Utility class - prevent instantiation
  }

  public static void setRequestId(String requestId) {
    REQUEST_ID.set(requestId);
  }

  public static String getRequestId() {
    return REQUEST_ID.get();
  }

  public static void clear() {
    REQUEST_ID.remove();
  }
}
