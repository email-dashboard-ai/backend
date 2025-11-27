package org.example.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    description = "JWT auth description",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {

  @Value("${swagger.server.url:http://localhost:3000}")
  private String serverUrl;

  @Value("${swagger.server.description:API Server}")
  private String serverDescription;

  @Bean
  public OpenAPI customOpenAPI() {
    Server server = new Server();
    server.setUrl(serverUrl);
    server.setDescription(serverDescription);

    return new OpenAPI()
        .info(
            new Info()
                .title("Email Dashboard API")
                .version("1.0")
                .description("OpenAPI documentation for Email Dashboard with Gmail Integration")
                .contact(new Contact().name("Dev Team").email("dev@example.com")))
        .servers(List.of(server))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }
}
