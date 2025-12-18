package org.example.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.example.util.RequestContext;
import org.example.util.RequestIdGenerator;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that generates a unique request ID (UUIDv7) for each incoming request. The request ID is:
 * - Added to the response header (X-Request-Id) - Stored in ThreadLocal for access throughout the
 * request - Added to SLF4J MDC for log correlation
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  private static final String MDC_REQUEST_ID_KEY = "requestId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // Check if client provided a request ID, otherwise generate one
    String requestId = request.getHeader(REQUEST_ID_HEADER);
    if (requestId == null || requestId.isBlank()) {
      requestId = RequestIdGenerator.generate();
    }

    // Store in ThreadLocal for access throughout the request
    RequestContext.setRequestId(requestId);

    // Add to MDC for log correlation
    MDC.put(MDC_REQUEST_ID_KEY, requestId);

    // Add to response header
    response.setHeader(REQUEST_ID_HEADER, requestId);

    try {
      log.debug(
          "Request started: {} {} [requestId={}]",
          request.getMethod(),
          request.getRequestURI(),
          requestId);
      filterChain.doFilter(request, response);
    } finally {
      // Clean up ThreadLocal and MDC to prevent memory leaks
      RequestContext.clear();
      MDC.remove(MDC_REQUEST_ID_KEY);
    }
  }
}
