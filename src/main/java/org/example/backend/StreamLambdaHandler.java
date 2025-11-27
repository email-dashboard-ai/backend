package org.example.backend;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.example.EmailApplication;

public class StreamLambdaHandler implements RequestStreamHandler {
  private static SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler;

  static {
    try {
      // For Cloudflare + Lambda Function URL, we use HTTP API v2
      handler = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(EmailApplication.class);

      // CRITICAL: Force UTF-8 for static resources (Swagger UI)
      // This prevents "Invalid or unexpected token" errors when serving JS files
      handler.onStartup(
          servletContext -> {
            jakarta.servlet.FilterRegistration.Dynamic encodingFilter =
                servletContext.addFilter(
                    "encodingFilter", new org.springframework.web.filter.CharacterEncodingFilter());
            encodingFilter.setInitParameter("encoding", "UTF-8");
            encodingFilter.setInitParameter("forceEncoding", "true");
            encodingFilter.addMappingForUrlPatterns(
                java.util.EnumSet.of(
                    jakarta.servlet.DispatcherType.REQUEST, jakarta.servlet.DispatcherType.FORWARD),
                true,
                "/*");
          });
    } catch (ContainerInitializationException e) {
      // if we fail here. We re-throw the exception to force another cold start
      e.printStackTrace();
      throw new RuntimeException("Could not initialize Spring Boot application", e);
    }
  }

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    handler.proxyStream(inputStream, outputStream, context);
  }
}
